package jobs;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

public class Convolution {
    private String jobName = "CONVOLUTION_";
    private static ArrayList<ArrayList<Float>> kernel = new ArrayList<ArrayList<Float>>();

    static {
        kernel.add(new ArrayList<>(Arrays.asList(-1f, -1f, -1f)));
        kernel.add(new ArrayList<>(Arrays.asList(-1f, 8f, -1f)));
        kernel.add(new ArrayList<>(Arrays.asList(-1f, -1f, -1f)));
    }

    public Convolution(ArrayList<ArrayList<Float>> kernel) {
        Convolution.kernel = kernel;
    }

    public BufferedImage applyConvolution(BufferedImage inputImage) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int kernelWidth = kernel.size();
        int kernelHeight = kernel.get(0).size();

        BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float[] rgb = new float[3];

                // Prolazak kroz kernel
                for (int i = 0; i < kernelWidth; i++) {
                    for (int j = 0; j < kernelHeight; j++) {
                        int pixelX = x + i - kernelWidth / 2;
                        int pixelY = y + j - kernelHeight / 2;

                        // Provera da li smo unutar granica slike
                        if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
                            int pixelColor = inputImage.getRGB(pixelX, pixelY);
                            rgb[0] += ((pixelColor >> 16) & 0xFF) * kernel.get(i).get(j);
                            rgb[1] += ((pixelColor >> 8) & 0xFF) * kernel.get(i).get(j);
                            rgb[2] += (pixelColor & 0xFF) * kernel.get(i).get(j);
                        }
                    }
                }

                // Postavi novi piksel u rezultujuću sliku
                int red = Math.min(Math.max((int) rgb[0], 0), 255);
                int green = Math.min(Math.max((int) rgb[1], 0), 255);
                int blue = Math.min(Math.max((int) rgb[2], 0), 255);

                int newPixel = (red << 16) | (green << 8) | blue;
                outputImage.setRGB(x, y, newPixel);
            }
            System.out.println(this.jobName + " is running - " + x + "/" + width);
        }

        return outputImage;
    }

    public static void main(String[] args) {
        try {
            if (args.length != 3) {
                System.out.println("Please provide a path to the input image and kernel file.");
                return;
            }
            ArrayList<ArrayList<Float>> kernel = new ArrayList<ArrayList<Float>>();
            File kernelFile = new File(args[0]);
            for (String line : Files.readAllLines(kernelFile.toPath())) {
                ArrayList<Float> row = new ArrayList<Float>();
                for (String value : line.split(" ")) {
                    row.add(Float.parseFloat(value));
                }
                kernel.add(row);
            }
            String inputFileName = args[1].substring(args[1].lastIndexOf(File.separator) + 1);
            // System.out.println(
            //         "Starting convolution for "
            //                 + inputFileName
            //                 + " with kernel "
            //                 + kernelFile.getName());
            Convolution convolution = new Convolution(kernel);
            convolution.jobName += inputFileName;
            // Učitaj sliku
            BufferedImage inputImage = ImageIO.read(new File(args[1]));

            // Primeni konvoluciju na sliku
            BufferedImage outputImage = convolution.applyConvolution(inputImage);

            String[] parts = inputFileName.split("\\.");
            String extension = inputFileName.split("\\.")[parts.length - 1];
            // Sačuvaj rezultujuću sliku
            ImageIO.write(
                    outputImage,
                    extension,
                    new File(args[2] + File.separator + "output_" + inputFileName));

            // System.out.println("Konvolucija uspešno završena.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

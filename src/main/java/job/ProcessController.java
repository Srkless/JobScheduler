package job;

import java.util.HashMap;
import java.util.function.Function;

public class ProcessController {
    private HashMap<String, Function<Process, Boolean>> controller =
            new HashMap<String, Function<Process, Boolean>>();

    public ProcessController() {
        addJobs();
    }

    public boolean execute(String command, Process process) {
        if (controller.containsKey(command)) {
            return controller.get(command).apply(process);
        }
        return false;
    }

    public void addJobs() {
        controller.put("PAUSE", this::pauseJob);
        controller.put("STOP", this::stopJob);
        controller.put("RESUME", this::resumeJob);
    }

    // TODO: napraviti da radi sa procesom, a da se status azurira ako je uspejsno izvrsena
    // operacija
    private boolean pauseJob(Process process) {
        try {
            System.out.println(
                    "Pausing job" + " " + process + " " + process.isAlive() + " " + process.pid());
            if (process != null && process.isAlive()) {
                String command = "kill -STOP " + process.pid();
                Runtime.getRuntime().exec(command.split(" "));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean stopJob(Process process) {
        process.destroy();
        if (process.isAlive()) {
            return false;
        }
        return true;
    }

    public boolean resumeJob(Process process) {
        try {
            System.out.println(
                    "Resuming job" + " " + process + " " + process.isAlive() + " " + process.pid());
            if (process != null && process.isAlive()) {
                String command = "kill -CONT " + process.pid();
                Runtime.getRuntime().exec(command.split(" "));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

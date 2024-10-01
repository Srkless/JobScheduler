package rabbitmq;

import com.rabbitmq.client.*;

public class RabbitMQHandler {

    private static String QUEUE_NAME = "Job_";

    public void write(String json, String queneName) {

        try {
            Connection connection = ConnectionFactoryUtil.createConnection();
            // while (!connection.isOpen()) {
            // Thread.sleep(100);
            // }
            Channel channel = connection.createChannel();

            channel.basicPublish("", QUEUE_NAME + queneName, null, json.getBytes("UTF-8"));

            channel.close();
            connection.close();

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public String read(String queneName) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        // channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        GetResponse response;
        String lastMessage = null;
        if ((response = channel.basicGet(QUEUE_NAME + queneName, false)) != null) {
            byte[] body = response.getBody();
            lastMessage = new String(body, "UTF-8");
            channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
        }

        channel.close();
        connection.close();
        if (lastMessage == null) return null;
        return lastMessage;
    }

    public static void createQueue(String jobName) {

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");

            try (Connection connection = factory.newConnection();
                    Channel channel = connection.createChannel()) {

                String newQueueName = "Job_" + jobName;

                // Deklariranje novog reda
                channel.queueDeclare(newQueueName, true, false, false, null);
                System.out.println("Novi red '" + newQueueName + "' je uspješno kreiran.");
            }

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static void deleteQueue(String queueName) {

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost"); // Set the host to your RabbitMQ instance
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // Delete the specified queue
            channel.queueDelete(queueName);

            System.out.println(
                    "Queue '" + QUEUE_NAME + queueName + "' has been deleted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

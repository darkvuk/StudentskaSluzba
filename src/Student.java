import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

public class Student implements AutoCloseable {

    private Connection connection;
    private Channel channel;
    private String requestQueueName = "studentska_sluzba";

    public Student() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    private static String podaci(){

        Scanner scan = new Scanner(System.in);

        System.out.print(" Ime: ");
        String ime = scan.nextLine();
        System.out.print(" Prezime: ");
        String prezime = scan.nextLine();
        System.out.print(" Broj indeksa: ");
        String indeks = scan.nextLine();
        System.out.print(" Godina upisa: ");
        String god_upisa = scan.nextLine();
        System.out.print(" Studijska godina: ");
        String stud_god = scan.nextLine();
        System.out.print(" Semestar: ");
        String semestar = scan.nextLine();
        System.out.print(" Predmet: ");
        String predmet = scan.nextLine();

        String input = ime + "*" + prezime + "*" + indeks + "*" + god_upisa + "*" + stud_god + "*" + semestar +
                "*" + predmet;


        return input;
    }

    public static void main(String[] argv){

        try (Student prijava = new Student()) {
            String upit = podaci();
            System.out.println("\n Proslijedjeno studentskoj sluzbi.");
            String response = prijava.call(upit);
            System.out.println("" + response);
        } catch (Exception e) {
            System.out.println("Error");
        }
    }

    public String call(String message) throws IOException, InterruptedException {
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.take();
        channel.basicCancel(ctag);
        return result;
    }

    public void close() throws IOException {
        connection.close();
    }
}

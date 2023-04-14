import com.rabbitmq.client.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class StudentskaSluzba {

    private static final String RPC_QUEUE_NAME = "studentska_sluzba";

    private static boolean postoji_u_bazi(String ime_baze, String check){

        boolean flag = false;

        File file1 = new File(ime_baze);
        try {
            Scanner scan1 = new Scanner(file1);
            int lineNum = 0;
            while (scan1.hasNextLine()) {
                String line = scan1.nextLine();
                lineNum++;
                if (line.equals(check)) {
                    flag = true;
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Jebem ti gresku");
        }
        return flag;
    }

    private static void upisi_u_bazu(String ime_baze, String linija){

        FileWriter fw = null;
        try {
            fw = new FileWriter(ime_baze, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fw.write("\n");
            fw.write(linija);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void azuriraj_bazu(String ime_baze, String input, String item){
        boolean flag = postoji_u_bazi(ime_baze, input);
        if(!flag) {
            upisi_u_bazu(ime_baze, input);
            flag = postoji_u_bazi(ime_baze, input);
            if(flag)
                System.out.println( item + " je dodat u bazu podataka.");
            else
                System.out.println( item + " nije dodat u bazu podataka.");
        } else{
            System.out.println( item + " vec postoji u bazi podataka.");
        }
    }

    private static String prijava_ispita(String message) {

        String output = "";

        try {
            String parts[] = message.split("*");
            String ime = parts[0];
            String prezime = parts[1];
            int indeks = Integer.parseInt(parts[2]);
            int godina = Integer.parseInt(parts[3]) % 100;
            String studijska_godina = parts[4];
            int semestar = Integer.parseInt(parts[5]);
            String predmet = parts[6];

            String check1 = ime + " " + prezime + " " + indeks + " " + godina;

            String check2 = semestar + " " + predmet;

            String check3 = ime + " " + prezime + " " + indeks + " " + godina + " " + studijska_godina + " " +
                    semestar + " " + predmet;

            boolean flag_student = postoji_u_bazi("studenti.txt", check1);
            boolean flag_predmet = postoji_u_bazi("predmeti.txt", check2);
            boolean flag_prijava = postoji_u_bazi("prijave.txt", check3);

            if (flag_predmet && !flag_prijava && flag_student) {
                azuriraj_bazu("prijave.txt", check3, "Prijavljen ispit");
                output = "Student je uspjesno prijavio ispit.";
            } else {
                output = "Student nije uspjesno prijavio predmet \n";
                System.out.println(" " + output);
                if (!flag_student)
                    output += " - Ne postoji student sa navedenim podacima.\n";
                if (!flag_predmet)
                    output += " - Ne postoji predmet sa navedenim podacima.\n";
                if (flag_prijava)
                    output += " - Student je vec prijavio ovaj predmet u studijskoj " + studijska_godina + ". godini.\n";
            }

        } catch (Exception e){
            output = "Uneseni podaci nisu validni.";
            System.out.println(output);
        }

        return output;

    }

    public static void prijava() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);

            channel.basicQos(1);

            System.out.println("\n Cekam prijavu studenta");

            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                String response = "";

                try {
                    String message = new String(delivery.getBody(), "UTF-8");
                    System.out.println(" Podaci su primljeni.");
                    System.out.println(" Vrsi se provjera podataka.");
                    Thread.sleep(2000);
                    response = prijava_ispita(message);
                } catch (RuntimeException | InterruptedException e) {
                    System.out.println(" [.] " + e.toString());
                } finally {
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };

            channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> { }));

            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    public static void main(String[] argv) throws Exception {


        System.out.println(" Izaberi opciju:");
        System.out.println(" [3] Prijava predmeta");

        System.out.print("\n Unos: ");
        Scanner scan = new Scanner(System.in);
        int i = scan.nextInt();

        switch(i){
            case 3: prijava(); break;
            default: System.out.println(" Nevalidan unos"); break;
        }


    }

}

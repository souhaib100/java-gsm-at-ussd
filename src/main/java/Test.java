import java.util.ArrayList;

public class Test {


    public static void main(String[] args) {
        String phoneNbr = "+9999999999";
        Gsm gsm = new Gsm();
        String[] ports = gsm.getSystemPorts();
        for (String port : ports) { // list all the available ports
            System.out.println(port);
        }
        gsm.initialize(ports[2]); // you must choose the correct port
//        gsm.sendSms(phoneNbr, "Hello world");
//        ArrayList<Sms> sms = gsm.readSms();
//        for (Sms s : sms) { // list all the available ports
//            System.out.println(s);
//        }
//        gsm.deleteAllSms(Gsm.SmsStorage[0]);
        gsm.closePort(); // Remember to close the port
    }


}

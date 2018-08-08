import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.google.common.base.Splitter;
import com.google.common.primitives.Longs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

public class Gsm {

    private SerialPort serialPort;
    private Logger logger;
    private String result;

    /**
     * Execute AT command
     *
     * @param at : the AT command
     * @return String contains the response
     */
    public String executeAT(String at) {
        at = at + "\r\n";
        result = "";
        serialPort.writeBytes((at).getBytes(), at.getBytes().length);
        while (result.equals("")) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Execute USSD command
     *
     * @param ussd : the USSD command
     * @return String contains the response
     */
    public String executeUSSD(String ussd) {
        String cmd = "at+cusd=1,\"" + ussd + "\", 15\r\n";
        result = "";
        serialPort.writeBytes((cmd).getBytes(), cmd.getBytes().length);
        while (result.equals("")) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (result.contains("ERROR")) {
            return result;
        }
        while (result.contains("OK")) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String str = "";
        if (result.contains("+CUSD")) {
            str = result.substring(12, result.length() - 6);
            //String[] arr = str.split("(?<=\\G....)");
            Iterable<String> arr = Splitter.fixedLength(4).split(str);
            str = "";
            for (String s : arr) {
                int hexVal = Integer.parseInt(s, 16);
                str += (char) hexVal;
            }
        }
        return str;
    }

    /**
     * Read the sms stored in the sim card
     *
     * @return ArrayList contains the sms
     */
    public ArrayList<Sms> readSms() {
        executeAT("ATE=1");
        if (executeAT("AT+CMGF=1").contains("ERROR")) {
            return null;
        } else {
            executeAT("AT+CMGL=\"ALL\"");
            //serialPort.writeBytes((cmd).getBytes(), cmd.getBytes().length);
            ArrayList<Sms> str = new ArrayList<>();
            int waiting = 0;
            while (!result.contains("+CMGL") || waiting < 10) {
                //String[] arr = str.split("(?<=\\G....)");
                System.out.println(result);
                if (result.contains("+CMGL")) {
                    String[] strs = result.replace("\"", "").split("(?:,)|(?:\r\n)");
                    System.out.println(Arrays.toString(strs));
                    Sms sms;
                    for (int i = 1; i < strs.length - 1; i++) {
                        //String str1 = strs[i];
                        sms = new Sms();
                        sms.setId(Integer.parseInt(strs[i].charAt(strs[i].length() - 1) + ""));
                        i++;
                        sms.setStatus(strs[i]);
                        i++;
                        sms.setPhone_num(strs[i]);
                        i++;
                        sms.setPhone_name(strs[i]);
                        i++;
                        sms.setDate(strs[i]);
                        i++;
                        sms.setTime(strs[i]);
                        i++;
                        if (Longs.tryParse(strs[i].substring(0, 2)) != null) { //get the message UNICODE
                            Iterable<String> arr = Splitter.fixedLength(4).split(strs[i]);
                            String con = "";
                            for (String s : arr) {
                                int hexVal = Integer.parseInt(s, 16);
                                con += (char) hexVal;
                            }
                            sms.setContent(con);
                        } else {//get the message String
                            sms.setContent(strs[i]);
                        }
                        if (!strs[i + 1].equals("") && !strs[i + 1].startsWith("+")) {
                            i++;
                            sms.setContent(sms.getContent() + "\n" + strs[i]);
                            i++;
                        }
                        str.add(sms);
                    }
                    break;
                } else {
                    try {
                        Thread.sleep(1000);
                        waiting++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return str;
        }

    }

    /**
     * Send an sms
     *
     * @param num the destination number
     * @param sms the body of the sms
     * @return ?
     */
    public String sendSms(String num, String sms) {
        String res = "";
        executeAT("ATE=0");
        executeAT("AT+CSCS=\"GSM\"");
        if (executeAT("AT+CMGF=1").contains("ERROR")) {
            return null;
        } else {
            executeAT("AT+CMGS=\"" + num + "\"");
            executeAT(sms);
            executeAT(Character.toString((char) 26));
            System.out.println(result);
        }
        return res;
    }

    /**
     * Initialize the connection
     *
     * @param port the port name
     * @return true if port was opened successfully
     */
    public boolean initialize(String port) {
        logger = Logger.getLogger("SP1");
        logger.info("Application start");
        serialPort = SerialPort.getCommPort(port);
        if (serialPort.openPort()) {
            logger.info("Port \"" + serialPort.getSystemPortName() + "\" was opened");
            serialPort.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                }

                @Override
                public void serialEvent(SerialPortEvent event) {
                    logger.info("receiving data");
                    byte[] msg = new byte[serialPort.bytesAvailable()];
                    serialPort.readBytes(msg, msg.length);
                    String res = new String(msg);
//                    System.out.println(res);
                    result = res;
                }
            });
            return true;
        } else

        {
            logger.warning("Failed to open port \"" + serialPort.getSystemPortName() + "\", application stopped.");
            return false;
        }

    }

    /**
     * Return list of the available port
     *
     * @return list contains list of the available port
     */
    public String[] getSystemPorts() {
        String[] systemPorts = new String[SerialPort.getCommPorts().length];
        for (int i = 0; i < systemPorts.length; i++) {
            systemPorts[i] = SerialPort.getCommPorts()[i].getSystemPortName();
        }
        return systemPorts;
    }

    /**
     * Close the connection
     *
     * @return true if port was closed successfully
     */
    public boolean closePort() {
        return serialPort.closePort();
    }
}

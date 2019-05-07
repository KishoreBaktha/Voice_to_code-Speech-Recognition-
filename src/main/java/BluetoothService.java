import net.sf.cglib.core.Local;

import java.io.IOException;
import java.util.Set;
import java.util.Vector;
import javax.bluetooth.*;

//import org.sputnikdev.bluetooth.manager.BluetoothManager;
//import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
//import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

public class BluetoothService {
    //public static final Vector<RemoteDevice> devicesDiscovered = new Vector();
    public static String deviceBluetoothAddress = "64A2F905CEFF";
    public static boolean bluetoothPaused = false;

    public static Vector<RemoteDevice> discoverDevices(SpeechService speechService, Dictionary dictionary)throws IOException, InterruptedException
    {
        final Object inquiryCompletedEvent = new Object();
        Vector<RemoteDevice> devicesDiscovered = new Vector();
        //devicesDiscovered.clear();

        DiscoveryListener listener = new DiscoveryListener() {
            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                System.out.println("Device " + btDevice.getBluetoothAddress() + " found");
                devicesDiscovered.addElement(btDevice);
                try {
                    System.out.println("     name " + btDevice.getFriendlyName(false));
                    if (btDevice.getBluetoothAddress().equals(deviceBluetoothAddress)) {

                        if (dictionary.paused && BluetoothService.bluetoothPaused) {
                            BluetoothService.bluetoothPaused = false;
                            dictionary.generateCode("restart");
                        }
                    }
                } catch (IOException cantGetDeviceName) {
                }

            }

            public void inquiryCompleted(int discType) {
                try {
                    BluetoothService.discoverDevices(speechService, dictionary);

                    System.out.println("Device Inquiry completed!");

                    synchronized(inquiryCompletedEvent){
                        inquiryCompletedEvent.notifyAll();
                        System.out.println(devicesDiscovered.size() +  " device(s) found");

                        for(RemoteDevice device: devicesDiscovered) {
                            if (device.getBluetoothAddress().equals(deviceBluetoothAddress)) {
                                System.out.println("Preconfigured device found.");
                                if (dictionary.paused && BluetoothService.bluetoothPaused) {
                                    BluetoothService.bluetoothPaused = false;
                                    dictionary.generateCode("restart");
                                }
                                return;
                            }
                        }
                        System.out.println("Preconfigured device not found. Pausing Voice to Code...");

                        if (!dictionary.paused) {
                            dictionary.generateCode("pause");
                            BluetoothService.bluetoothPaused = true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            public void serviceSearchCompleted(int transID, int respCode) {
            }

            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
                System.out.println(transID);
                System.out.println(servRecord);
            }
        };

        synchronized(inquiryCompletedEvent) {
            boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, listener);
            speechService.bluetoothRunning = true;
            if (started) {
                System.out.println("wait for device inquiry to complete...");
            }
        }

        return devicesDiscovered;
    }
}

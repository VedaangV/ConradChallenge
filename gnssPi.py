import time
import serial
import pynmea2
import re
#COMMIT2
class AT:
    def __init__(self, port='/dev/ttyS0', baud_rate=115200):
        self.port = port
        self.baud_rate = baud_rate
        self.ser = None
        self.initialize_gnss()

    def initialize_gnss(self):
        try:
            self.ser = serial.Serial(self.port, self.baud_rate, timeout=1)
            self.send_at_command('AT+CGNSSPORTSWITCH=0,0')
            self.send_at_command('AT+CGNSSTST=0')
            self.send_at_command('AT+CGNSSPWR=0')
        except serial.SerialException as e:
            raise Exception(f"Error initializing GNSS: {e}")

    def open_serial_connection(self):
        try:
            self.ser = serial.Serial(self.port, self.baud_rate, timeout=1)
            return self.send_at_command('AT') == "OK"
        except serial.SerialException as e:
            raise Exception(f"Error opening serial connection: {e}")

    def send_at_command(self, command, wait_for_response=True):
        try:
            encoded_command = command.encode('utf-8') + b'\r\n'
            self.ser.write(encoded_command)
            print(f"Sent command: {command}")

            if wait_for_response:
                response = self.ser.readlines()[1].decode('utf-8').strip()
                print(f"Response: {response}")
                return response
            else:
                return None
        except serial.SerialException as e:
            raise Exception(f"Error sending AT command: {e}")

    def gnss_pwr_open(self, enable):
        command = f'AT+CGNSSPWR={int(enable)}'
        return self.send_at_command(command) == "OK"

    def gnss_layout(self):
        return self.send_at_command('AT+CGNSSTST=1') == "OK"

    def gnss_layout_switch(self):
        time.sleep(10)
        self.send_at_command('AT+CGNSSPORTSWITCH=0,1', wait_for_response=False)
        return True

    def gnss_params(self):
        try:
            response = str(self.ser.readline(), encoding='utf-8')
            if response.startswith("$GNRMC"):
                rmc = pynmea2.parse(response)
                return rmc
            else:
                return None
        except serial.SerialException as e:
            raise Exception(f"Error reading GNSS params: {e}")
        except pynmea2.ParseError:
            return None


if __name__ == '__main__':
    try:
        at_instance = AT()
        if at_instance.open_serial_connection():
            at_instance.gnss_pwr_open(1)
            at_instance.gnss_layout()
            at_instance.gnss_layout_switch()
            
            print("Waiting for GNSS fix... (may take several minutes if indoors)")
            
            while True:
                response = at_instance.gnss_params()
                if response:
                    # Check if latitude and longitude are valid numbers
                    try:
                        lat = float(response.latitude)
                        lon = float(response.longitude)
                        if lat != 0.0 and lon != 0.0:
                            print(f"Latitude: {lat}, Longitude: {lon}")
                        else:
                            print("Waiting for GNSS fix...")
                    except (ValueError, TypeError):
                        print("Waiting for GNSS fix...")
                else:
                    print("")
                time.sleep(1)  # avoid flooding the console
                
    except KeyboardInterrupt:
        print("KeyboardInterrupt received. Cleaning up before exiting.")
        if at_instance.ser:
            at_instance.ser.close()
    except Exception as e:
        print(f"An error occurred: {e}")
    finally:
        print("Program terminated.")

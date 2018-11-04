# AndroidBluetoothExample
Android藍牙聊天通訊 <br/>
參考範例
https://examples.javacodegeeks.com/android/android-bluetooth-connection-example/#device_code

android藍牙傳輸<br/>

藍牙（IEEE 802.15）是一種低功耗、低成本的主從式短距無線傳輸通道，目前已經成為行動手持裝置與配件設備之間的標準無線通訊介面。<br/>
藍牙提供行動裝置與配件之間，可進行非同步資料傳輸及同步語音傳輸。<br/>
在Android中藍牙傳輸作業主要由以下類別進行處理：<br/>
BluetoothAdapter：藍牙適配器，代表本機運行的藍牙設備<br/>
BluetoothDevice：遠端藍牙設備。<br/>
BluetoothSocket：與遠端藍牙裝置之通訊介面。<br/>

BluetoothServerSocket：用來監聽來自遠端設備送來的Bluetooth Sockets。<br/>

UUID是由一組128位元的16進位數字所構成，用來提供資訊裝置服務唯一的識別碼。<br/>
UUID的標準型式包含32個16進位數字，以連字號分為五段，形式為8-4-4-4-12的32個字元。<br/>
使用標準藍牙通訊協定之藍牙裝置 UUID為 fa87c0d0-afac-11de-8a39-0800200c9a66，<br/>
如果要將藍牙裝置當作串列接口協定，則UUID要設為00001101-0000-1000-8000-00805F9B34FB <br/>


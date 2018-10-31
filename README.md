# Logger

将app日志保存到文件以方便debug

Application中初始化：  
 mLogger = Logger.newBuilder(this)  
     .setLogType(LogType.WARN)//日志输出类型  
     .withPrint(true)//是否输出到终端  
     .bulid();

 mLogger.cleanPreviewLog();//清除之前全部log文件  
 mLogger.start();//开始记录

 mLogger.stop();//停止记录

Class中引用：  
 LogUtil.v(TAG, "this is a verbose log");  
 LogUtil.d(TAG, "this is a debug log");  
 LogUtil.i(TAG, "this is a info log");  
 LogUtil.w(TAG, "this is a warn log");   
 LogUtil.e(TAG, "this is a error log");

数据将以txt形式保存在sdcard/Android/data/应用包名/files/目录  
10-31 19:56:49.520  V  MainActivity:  this is a verbose log  
10-31 19:56:49.521  D  MainActivity:  this is a debug log  
10-31 19:56:49.521  I  MainActivity:  this is a info log  
10-31 19:56:49.522  W  MainActivity:  this is a warn log  
10-31 19:56:49.522  E  MainActivity:  this is a error log  

注:
使用前需打开相应应用读写存储空间权限


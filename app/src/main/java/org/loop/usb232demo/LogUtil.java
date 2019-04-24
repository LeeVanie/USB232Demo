package org.loop.usb232demo;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * @className LogUtil
 * @author goubaihu
 * @function 将日志写入本地
 * @createTime 2019年4月24号
 */
public class LogUtil {

	public static String FileBasePath = "/storage/emulated/0/Android/data/org.loop.usb232demo/files/Documents/rs232.txt";
	private static LogUtil mLogWriter;
	private static SimpleDateFormat df;

	private LogUtil() {

	}

	/**
	 * 获取文件路径
	 *            路径
	 */
	private static String getFilePath() {
		return FileBasePath
				+ new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".txt";
	}

	public static LogUtil open() {
		try {
			if (mLogWriter == null) {
				mLogWriter = new LogUtil();
			}
			File mFile = new File(FileBasePath);
			if (!mFile.exists()) {
				File file2 = new File(mFile.getParent());
				file2.mkdirs();
			}
			if (!mFile.isDirectory()) {
//				deleteFile();
//				deleteFile2();
				mFile.createNewFile();
			}
			df = new SimpleDateFormat("[yy-MM-dd hh:mm:ss]: ");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mLogWriter;
	}
	
	/**
	 * 删除一天之前的为日志文件
	 * 
	 * @throws Exception
	 */
	public static LogUtil deleteFile() throws Exception {
		if (mLogWriter == null) {
			mLogWriter = new LogUtil();
		}
		File pathRoot = new File(FileBasePath);
		if (pathRoot.exists() && pathRoot.listFiles().length > 0) {
			for (File file : pathRoot.listFiles()) {
				if (file.isFile()) {
					String s = file.getName().substring(0,
							file.getName().length() - 4);
					Date date = new SimpleDateFormat("yyyyMMdd").parse(s);
					if ((new Date().getTime() - date.getTime()) > 2 * 24 * 60 * 60 * 1000) {
						file.delete();
					}
				}

			}
		}
		return mLogWriter;
	}

	public static void appendMethodB(String log) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(FileBasePath, true);
			writer.write(df.format(new Date()));
			writer.write(log);
			writer.write("\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				writer = null;
			}
		}
	}

    //参数一、文件的byte流
    //参数二、文件要保存的路径
    //参数三、文件保存的名字
    public static void saveFile(byte[] bfile, String filePath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;

        File file = null;
        try {
            //通过创建对应路径的下是否有相应的文件夹。
            File dir = new File(filePath);
            if (!dir.exists()) {// 判断文件目录是否存在
                //如果文件存在则删除已存在的文件夹。
                dir.mkdirs();
            }

            //如果文件存在则删除文件
            file = new File(filePath, fileName);
            if(file.exists()){
                file.delete();
            }
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            //把需要保存的文件保存到SD卡中
            bos.write(bfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

}

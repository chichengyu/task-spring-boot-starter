package com.job.excel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Excel数据格式处理适配器
 */
public interface ExcelHandlerAdapter {
    String separator = ".";

    /**
     * 格式化
     * @param value 单元格数据值(列为文件时,为null)
     * @param fileStream 文件流(列为文件时,存在)
     * @return 处理后的值
     */
    Object format(Object value,byte[] fileStream);

    /**
     * 获取图像文件名称
     * @param photoByte 图像字节流
     * @return 后缀名
     */
    default String getImageFileName(byte[] photoByte){
        return UUID.randomUUID().toString().replace("-","") + separator + getImageExtend(photoByte);
    }

    /**
     * 获取图像后缀
     * @param photoByte 图像字节流
     * @return 后缀名
     */
    default String getImageExtend(byte[] photoByte){
        String strFileExtendName = "jpg";
        if ((photoByte[0] == 71) && (photoByte[1] == 73) && (photoByte[2] == 70) && (photoByte[3] == 56) && ((photoByte[4] == 55) || (photoByte[4] == 57)) && (photoByte[5] == 97)){
            strFileExtendName = "gif";
        } else if ((photoByte[6] == 74) && (photoByte[7] == 70) && (photoByte[8] == 73) && (photoByte[9] == 70)){
            strFileExtendName = "jpg";
        } else if ((photoByte[0] == 66) && (photoByte[1] == 77)){
            strFileExtendName = "bmp";
        } else if ((photoByte[1] == 80) && (photoByte[2] == 78) && (photoByte[3] == 71)){
            strFileExtendName = "png";
        }
        return strFileExtendName;
    }

    /**
     * 将文件保存至指定路径
     * @param fileStream 文件字节流
     * @param filePath 文件路径
     */
    default void writeBytesToFile(byte[] fileStream,String filePath) throws IOException {
        FileOutputStream out = new FileOutputStream(filePath);
        out.write(fileStream);
        out.close();
    }
}

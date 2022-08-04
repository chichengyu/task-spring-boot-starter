package com.job.excel;

import com.job.excel.annotation.Excel;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel工具类
 * @author xiaochi
 */
public class ExcelUtil<T> {
    private static final int START_ROW_NUM=1;// 默认第1行，表头不算
    private Class<T> clazz;
    private Field[] fields;

    public ExcelUtil(Class<T> clazz){
        this.clazz = clazz;
        this.fields = clazz.getDeclaredFields();
    }

    /**
     * 读取 Excel 数据
     * param inputStream 文件流
     */
    public List<T> read(InputStream inputStream) throws Exception {
        return read(inputStream,START_ROW_NUM);
    }

    /**
     * 读取 Excel 数据
     * param filePath excel文件所在路径（一般先上传在读取）
     */
    public List<T> read(String filePath) throws Exception {
        InputStream is = new FileInputStream(filePath);
        return read(is,START_ROW_NUM);
    }

    /**
     * 读取 Excel 数据
     * param filePath excel文件所在路径（一般先上传在读取）
     * param startRowNum 从excel哪一行开始读
     */
    public List<T> read(String filePath, int startRowNum) throws Exception {
        InputStream is = new FileInputStream(filePath);
        return read(is,startRowNum);
    }

    /**
     * 读取 Excel 数据
     * param inputStream 获取上传时的 inputStream 转成 FileInputStream
     * param startRowNum 从excel哪一行开始读(从列属性开始计算)
     */
    public List<T> read(InputStream inputStream, int startRowNum) throws Exception {
        Workbook wb = WorkbookFactory.create(inputStream);
        Map<String, String> fieldsAndHeader = getFieldsAndHeader();
        List<T> backList = new ArrayList<>();
        List<String> coloumVals = new ArrayList<>();
        // 循环工作表Sheet
        Sheet hssfSheet = wb.getSheetAt(0);
        if (hssfSheet != null) {
            // coloumNum 获取表头名称 对应关系  从1行开始先读取列属性,表头不算
            Row row = hssfSheet.getRow(startRowNum);
            int coloumNum = row.getPhysicalNumberOfCells();// 总列数
            for (int i = 0; i < coloumNum; i++){
                String coloumVal = row.getCell(i).toString();
                for (Map.Entry<String, String> entry : fieldsAndHeader.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (value != null && value.equals(coloumVal)){
                        coloumVals.add(i,key);
                        break;
                    }
                }
            }
            if (coloumVals.size() > 0){
                // 循环行Row
                for (int rowNum = startRowNum+1; rowNum <= hssfSheet.getLastRowNum(); rowNum++) {
                    Row sheetRow = hssfSheet.getRow(rowNum);
                    T t = this.clazz.newInstance();
                    for (int i = 0, leng = coloumVals.size(); i < leng; i++) {
                        String attrVal = coloumVals.get(i);
                        for (Field field : this.fields){
                            String attrPojo = field.getName();
                            if (field.isAnnotationPresent(Excel.class) && attrVal.equals(attrPojo)){
                                Excel excel = field.getAnnotation(Excel.class);
                                field.setAccessible(true);
                                Class<?> fieldType = field.getType();
                                Object val = getCellValue(sheetRow.getCell(i));
                                if (val == null || "".equals(val)){
                                    continue;
                                }
                                String suffix = excel.suffix();
                                if (!"".equals(suffix) && val.toString().contains(suffix)){
                                    val = val.toString().replace(suffix,"");
                                }
                                if (excel.readCoverDefaultValue()){
                                    String defaultValue = excel.defaultValue();
                                    if (!"".equals(defaultValue) && val.toString().contains(defaultValue)){
                                        val = val.toString().replace(defaultValue,"");
                                        if ("".equals(val)){
                                            field.set(t,null);
                                            continue;
                                        }
                                    }
                                }
                                String converterExp = excel.readConverterExp();
                                if (!"".equals(converterExp)){
                                    val = reverseByExp(String.valueOf(val),converterExp);
                                }
                                try {
                                    if (String.class == fieldType){
                                        field.set(t,val.toString());
                                    }else if (Integer.TYPE == fieldType || Integer.class == fieldType){
                                        field.set(t,Integer.parseInt(val.toString()));
                                    }else if (Long.TYPE == fieldType || Long.class == fieldType){
                                        field.set(t,Long.parseLong(val.toString()));
                                    }else if (Float.TYPE == fieldType || Float.class == fieldType){
                                        field.set(t,Float.valueOf(val.toString()));
                                    }else if (Double.TYPE == fieldType || Double.class == fieldType){
                                        field.set(t,Double.valueOf(val.toString()));
                                    }else if (BigDecimal.class == fieldType){
                                        field.set(t,BigDecimal.valueOf(Double.parseDouble(val.toString())));
                                    }else if (Date.class == fieldType){
                                        if (val instanceof String) {
                                            val = new SimpleDateFormat(excel.dateformat()).parse(((String) val).trim());
                                        } else if (val instanceof Double) {
                                            val = DateUtil.getJavaDate((Double) val);
                                        }
                                        field.set(t,val);
                                    }
                                }catch (Exception e){
                                    throw new RuntimeException("第"+(rowNum+1)+"行"+excel.name()+"列非法字符或值错误");
                                }
                            }
                        }
                    }
                    backList.add(t);
                }
            }
        }
        return backList;
    }

    /**
     * 获取单元格值
     * param cell
     */
    private Object getCellValue(Cell cell){
        if (cell == null){
            return cell;
        }
        Object value = "";
        try{
            CellType cellType = cell.getCellType();
            if (cellType == CellType.NUMERIC || cellType == CellType.FORMULA){
                value = cell.getNumericCellValue();
                if (DateUtil.isCellDateFormatted(cell)){
                    value = DateUtil.getJavaDate((Double) value); // POI Excel 日期格式转换
                }else {
                    if ((Double) value % 1 > 0) {
                        value = new BigDecimal(value.toString());
                    } else {
                        value = new DecimalFormat("0").format(value);
                    }
                }
            }else if (cellType == CellType.STRING) {
                value = cell.getStringCellValue();
            } else if (cellType == CellType.BOOLEAN) {
                value = cell.getBooleanCellValue();
            } else if (cellType == CellType.ERROR) {
                value = cell.getErrorCellValue();
            }
        }catch (Exception e){
            return value;
        }
        return value;
    }

    /**
     * 获取pojo实体对象被Excel注解属性
     */
    private Map<String,String> getFieldsAndHeader(){
        Map<String,String> fieldHeader = new HashMap<>();
        for (Field field : this.fields){
            if (field.isAnnotationPresent(Excel.class)){
                field.setAccessible(true);
                String attr = field.getName();// 实体属性
                Excel excel = field.getAnnotation(Excel.class);
                String attrName = excel.name();// 实体属性对应的表头名称
                fieldHeader.put(attr,attrName);
            }
        }
        return fieldHeader;
    }

    /**
     * 导出 excel
     * param fileName 文件名称
     * param data 数据
     */
    public String export(HttpServletResponse response, String fileName, List<T> data) throws Exception {
        OutputStream out = null;
        HSSFWorkbook wb = null;
        try{
            wb = createHSSFWorkbook(fileName, getHeaders(), data);
            fileName = (fileName.contains(".xls")||fileName.contains(".xlsx")) ? fileName : (fileName + ".xlsx");
            /* ajax方式是先生成一个文件在服务器端，前端进行下载
            out = new FileOutputStream(fileName);
            wb.write(out);
            return R.ok(fileName)*/;
            out = setResponseHeader(fileName,response).getOutputStream();
            wb.write(out);
            out.flush();
            out.close();
            return fileName;
        } finally {
            try{
                if (wb != null){
                    wb.close();
                }
                if (out != null){
                    out.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取 excel 表头名称
     */
    private List<String> getHeaders(){
        List<String> headers = new ArrayList<>();
        for (Field field : this.fields){
            if (field.isAnnotationPresent(Excel.class)){
                field.setAccessible(true);
                Excel excel = field.getAnnotation(Excel.class);
                headers.add(excel.name());
            }
        }
        return headers;
    }

    /** createHSSFWorkbook
     * param title 标题
     * param headers  表头
     * param data  数据
     */
    private HSSFWorkbook createHSSFWorkbook(String title, List<String> headers, List<T> data) throws Exception {
        //创建一个HSSFWorkbook，对应一个Excel文件
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
        //在workbook中添加一个sheet,对应Excel文件中的sheet
        HSSFSheet hssfSheet = hssfWorkbook.createSheet(title);
        //创建标题合并行
        hssfSheet.addMergedRegion(new CellRangeAddress(0,(short)0,0,(short)headers.size() - 1));
        //设置标题样式
        HSSFCellStyle style = hssfWorkbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);   //设置居中样式
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        //设置标题字体
        Font titleFont = hssfWorkbook.createFont();
        titleFont.setFontHeightInPoints((short) 14);
        style.setFont(titleFont);
        //设置值表头样式 设置表头居中
        HSSFCellStyle hssfCellStyle = hssfWorkbook.createCellStyle();
        hssfCellStyle.setAlignment(HorizontalAlignment.CENTER);   //设置居中样式
        hssfCellStyle.setBorderBottom(BorderStyle.THIN);
        hssfCellStyle.setBorderLeft(BorderStyle.THIN);
        hssfCellStyle.setBorderRight(BorderStyle.THIN);
        hssfCellStyle.setBorderTop(BorderStyle.THIN);
        //设置表内容样式
        //创建单元格，并设置值表头 设置表头居中
        HSSFCellStyle style1 = hssfWorkbook.createCellStyle();
        style1.setAlignment(HorizontalAlignment.CENTER);   //设置居中样式
        style1.setBorderBottom(BorderStyle.THIN);
        style1.setBorderLeft(BorderStyle.THIN);
        style1.setBorderRight(BorderStyle.THIN);
        style1.setBorderTop(BorderStyle.THIN);
        //产生标题行
        HSSFRow hssfRow = hssfSheet.createRow(0);
        HSSFCell cell = hssfRow.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        //产生表头
        HSSFRow row1 = hssfSheet.createRow(1);
        for (int i = 0, length = headers.size(); i < length; i++){
            HSSFCell hssfCell = row1.createCell(i);
            hssfCell.setCellValue(headers.get(i));
            hssfCell.setCellStyle(hssfCellStyle);
        }
        //创建内容
        for (int i = 0,length = data.size(); i < length; i++){
            row1 = hssfSheet.createRow(i + 2);
            T t = data.get(i);
            int k = 0;
            for (Field field : this.fields){
                if (field.isAnnotationPresent(Excel.class)){
                    Excel excel = field.getAnnotation(Excel.class);
                    // 设置列宽
                    hssfSheet.setColumnWidth(k,(int)((excel.width() + 0.72) * 256));
                    //将内容按顺序赋给对应列对象
                    HSSFCell hssfCell = row1.createCell(k++);
                    hssfCell.setCellStyle(style1);
                    field.setAccessible(true);
                    String defaultValue = excel.defaultValue();
                    Object val = field.get(t);
                    if ((val == null || "".equals(val)) && !"".equals(defaultValue)){
                        val = defaultValue;
                    }
                    if (val != null){
                        String suffix = excel.suffix();
                        String converterExp = excel.readConverterExp();
                        Class<?> fieldType = field.getType();
                        if (!"".equals(suffix)){
                            val += suffix;
                        }
                        if (!"".equals(converterExp)){
                            hssfCell.setCellValue(convertByExp(String.valueOf(val),converterExp));
                            continue;
                        }
                        if (String.class == fieldType){
                            hssfCell.setCellValue(val.toString());
                        }else if (Integer.TYPE == fieldType || Integer.class == fieldType){
                            hssfCell.setCellValue(Integer.parseInt(val.toString()));
                        }else if (Long.TYPE == fieldType || Long.class == fieldType){
                            hssfCell.setCellValue(Long.parseLong(val.toString()));
                        }else if (Float.TYPE == fieldType || Float.class == fieldType){
                            hssfCell.setCellValue(Float.parseFloat(val.toString()));
                        }else if (Double.TYPE == fieldType || Double.class == fieldType){
                            hssfCell.setCellValue(Double.parseDouble(val.toString()));
                        }else if (BigDecimal.class == fieldType){
                            hssfCell.setCellValue(Double.parseDouble(val.toString()));
                        }else if (Date.class == fieldType){
                            if ("".equals(excel.dateformat())){
                                hssfCell.setCellValue(val.toString());
                            }else {
                                hssfCell.setCellValue(new SimpleDateFormat(excel.dateformat()).format(val));
                            }
                        }
                    }
                }
            }
        }
        return hssfWorkbook;
    }

    /**
     * 解析导出值 0=男,1=女,2=未知
     * param propertyValue 参数值
     * param converterExp 翻译注解
     */
    private String convertByExp(String propertyValue, String converterExp){
        String[] convertSource = converterExp.split(",");
        for (String item : convertSource){
            String[] itemArray = item.split("=");
            if (itemArray[0].equals(propertyValue)){
                return itemArray[1];
            }
        }
        return propertyValue;
    }

    /**
     * 反向解析导出值 0=男,1=女,2=未知
     * param propertyValue 参数值
     * param converterExp 翻译注解
     */
    private String reverseByExp(String propertyValue, String converterExp){
        String[] convertSource = converterExp.split(",");
        for (String item : convertSource){
            String[] itemArray = item.split("=");
            if (itemArray[1].equals(propertyValue)){
                return itemArray[0];
            }
        }
        return propertyValue;
    }

    /**
     * 设置响应头
     * param fileName
     * param response
     */
    private HttpServletResponse setResponseHeader(String fileName,HttpServletResponse response) throws Exception {
        fileName = URLEncoder.encode(fileName,"UTF-8");
        fileName = new String(fileName.getBytes(), "ISO8859-1");
        //response.setContentType("application/vnd.ms-excel");// 导出流
        response.setContentType("application/octet-stream;charset=ISO8859-1");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        response.addHeader("Pargam", "no-cache");
        response.addHeader("Cache-Control", "no-cache");
        return response;
    }

    //---------------------------------------------- 不使用注解 ---------------------------------------------------------
    /**
     * 导入 Excel 数据
     * param filePath excel文件所在路径（一般先上传在读取）
     * param fields 字段数组
     * param startRowNum 从excel哪一行开始读
     */
    public static List<Map<String,Object>> read2(String filePath, List<String> fields, int startRowNum) throws Exception {
        InputStream is = new FileInputStream(filePath);
        return read2(is,fields,startRowNum);
    }

    /**
     * 导入 Excel 数据
     * param filePath excel文件所在路径（一般先上传在读取）
     * param fields 字段数组
     */
    public static List<Map<String,Object>> read2(String filePath, List<String> fields) throws Exception {
        InputStream is = new FileInputStream(filePath);
        return read2(is,fields,START_ROW_NUM+1);
    }

    /**
     * 导入 Excel 数据
     * param inputStream 获取上传时的 inputStream 转成 FileInputStream
     * param fields 字段数组
     */
    public static List<Map<String,Object>> read2(InputStream inputStream, List<String> fields) throws Exception {
        return read2(inputStream,fields,START_ROW_NUM+1);
    }

    /**
     * 导入 Excel 数据
     * param inputStream 获取上传时的 inputStream 转成 FileInputStream
     * param fields 字段数组
     * param startRowNum 从excel哪一行开始读
     */
    public static List<Map<String,Object>> read2(InputStream inputStream, List<String> fields, int startRowNum) throws Exception {
        Workbook hssfWorkbook = WorkbookFactory.create(inputStream);
        List<Map<String,Object>> list = new ArrayList<>();
        // 循环工作表Sheet
        for (int numSheet = 0; numSheet < hssfWorkbook.getNumberOfSheets(); numSheet++) {
            Sheet hssfSheet = hssfWorkbook.getSheetAt(numSheet);
            if (hssfSheet == null) {
                continue;
            }
            // 循环行Row
            for (int rowNum = startRowNum; rowNum <= hssfSheet.getLastRowNum(); rowNum++) {
                Row hssfRow = hssfSheet.getRow(rowNum);
                Map<String,Object> map = new HashMap<>();
                for (int i = 0,leng=fields.size(); i < leng; i++) {
                    map.put(fields.get(i),hssfRow.getCell(i));
                }
                list.add(map);
            }
        }
        return list;
    }

    /**
     * 导出 excel
     * param response resp
     * param fileName 文件名称
     * param headers 表头信息[一维数组]
     * param data 数据[二维数组]
     */
    public static String export2(HttpServletResponse response,String fileName,String[] headers,String[][] data) throws Exception {
        OutputStream out = null;
        HSSFWorkbook wb = null;
        try{
            wb = getHSSFWorkbook(fileName, headers, data);
            fileName = (fileName.contains(".xls")||fileName.contains(".xlsx")) ? fileName : (fileName + ".xlsx");
            fileName = URLEncoder.encode(fileName,"UTF-8");
            fileName = new String(fileName.getBytes(), "ISO8859-1");
            //response.setContentType("application/vnd.ms-excel");// 导出流
            response.setContentType("application/octet-stream;charset=ISO8859-1");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.addHeader("Pargam", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            out = response.getOutputStream();
            wb.write(out);
            out.flush();
            out.close();
            return fileName;
        } finally {
            try{
                if (wb != null){
                    wb.close();
                }
                if (out != null){
                    out.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /** 获取 HSSFWorkbook
     * param title 标题
     * param headers  表头
     * param values  表中元素
     */
    private static HSSFWorkbook getHSSFWorkbook(String title, String[] headers, String [][] values){
        //创建一个HSSFWorkbook，对应一个Excel文件
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
        //在workbook中添加一个sheet,对应Excel文件中的sheet
        HSSFSheet hssfSheet = hssfWorkbook.createSheet(title);
        //创建标题合并行
        hssfSheet.addMergedRegion(new CellRangeAddress(0,(short)0,0,(short)headers.length - 1));
        //设置标题样式
        HSSFCellStyle style = hssfWorkbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);   //设置居中样式
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        //设置标题字体
        Font titleFont = hssfWorkbook.createFont();
        titleFont.setFontHeightInPoints((short) 14);
        style.setFont(titleFont);
        //设置值表头样式 设置表头居中
        HSSFCellStyle hssfCellStyle = hssfWorkbook.createCellStyle();
        hssfCellStyle.setAlignment(HorizontalAlignment.CENTER);   //设置居中样式
        hssfCellStyle.setBorderBottom(BorderStyle.THIN);
        hssfCellStyle.setBorderLeft(BorderStyle.THIN);
        hssfCellStyle.setBorderRight(BorderStyle.THIN);
        hssfCellStyle.setBorderTop(BorderStyle.THIN);
        //设置表内容样式
        //创建单元格，并设置值表头 设置表头居中
        HSSFCellStyle style1 = hssfWorkbook.createCellStyle();
        style1.setBorderBottom(BorderStyle.THIN);
        style1.setBorderLeft(BorderStyle.THIN);
        style1.setBorderRight(BorderStyle.THIN);
        style1.setBorderTop(BorderStyle.THIN);
        //产生标题行
        HSSFRow hssfRow = hssfSheet.createRow(0);
        HSSFCell cell = hssfRow.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        //产生表头
        HSSFRow row1 = hssfSheet.createRow(1);
        for (int i = 0; i < headers.length; i++) {
            HSSFCell hssfCell = row1.createCell(i);
            hssfCell.setCellValue(headers[i]);
            hssfCell.setCellStyle(hssfCellStyle);
        }
        //创建内容
        for (int i = 0; i <values.length; i++){
            row1 = hssfSheet.createRow(i +2);
            for (int j = 0; j < values[i].length; j++){
                //将内容按顺序赋给对应列对象
                HSSFCell hssfCell = row1.createCell(j);
                hssfCell.setCellValue(values[i][j]);
                hssfCell.setCellStyle(style1);
            }
        }
        return hssfWorkbook;
    }
}

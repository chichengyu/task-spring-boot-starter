package com.job.excel;

import com.job.excel.annotation.ExcelColumn;
import com.job.excel.annotation.ExcelHead;
import com.job.excel.annotation.ExcelSheet;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
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
import java.util.Objects;

/**
 * Excel工具类
 * @author xiaochi
 */
public class Excel<T> {
    private static final int START_ROW_NUM=1;// 默认第1行，表头不算
    private Style titleStyle;// 标题样式
    private Style headerStyle;// 表头样式
    private GridStyle gridStyle;// 表格样式
    private Class<T> clazz;
    private Field[] fields;

    @FunctionalInterface
    public interface Style{
        HSSFCellStyle execute(HSSFWorkbook hssfWorkbook);
    }

    @FunctionalInterface
    public interface GridStyle{
        HSSFCellStyle execute(HSSFWorkbook hssfWorkbook, Field field);
    }

    public Excel(Class<T> clazz){
        this.clazz = clazz;
        this.fields = clazz.getDeclaredFields();
    }

    public static <T>Excel<T> type(Class<T> clazz){
        return new Excel<>(clazz);
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
                            if (field.isAnnotationPresent(ExcelColumn.class) && attrVal.equals(attrPojo)){
                                ExcelColumn excelColumn = field.getAnnotation(ExcelColumn.class);
                                field.setAccessible(true);
                                Class<?> fieldType = field.getType();
                                Object val = getCellValue(sheetRow.getCell(i));
                                if (val == null || "".equals(val)){
                                    continue;
                                }
                                String suffix = excelColumn.suffix();
                                if (!"".equals(suffix) && val.toString().contains(suffix)){
                                    val = val.toString().replace(suffix,"");
                                }
                                if (excelColumn.readDefaultValue()){
                                    String defaultValue = excelColumn.defaultValue();
                                    if (!"".equals(defaultValue) && val.toString().contains(defaultValue)){
                                        val = val.toString().replace(defaultValue,"");
                                        if ("".equals(val)){
                                            field.set(t,null);
                                            continue;
                                        }
                                    }
                                }
                                String converExp = excelColumn.converExp();
                                if (!"".equals(converExp)){
                                    val = reverseByExp(String.valueOf(val),converExp);
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
                                            val = new SimpleDateFormat(excelColumn.dateformat()).parse(((String) val).trim());
                                        } else if (val instanceof Double) {
                                            val = DateUtil.getJavaDate((Double) val);
                                        }
                                        field.set(t,val);
                                    }
                                }catch (Exception e){
                                    throw new RuntimeException("第"+(rowNum+1)+"行"+ excelColumn.name()+"列非法字符或值错误");
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
            if (field.isAnnotationPresent(ExcelColumn.class)){
                field.setAccessible(true);
                String attr = field.getName();// 实体属性
                ExcelColumn excelColumn = field.getAnnotation(ExcelColumn.class);
                String attrName = excelColumn.name();// 实体属性对应的表头名称
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
            wb = createHSSFWorkbook(fileName, data);
            fileName = (fileName.contains(".xls")||fileName.contains(".xlsx")) ? fileName : (fileName + ".xlsx");
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

    /** createHSSFWorkbook
     * param title 标题
     * param headers  表头
     * param data  数据
     */
    private HSSFWorkbook createHSSFWorkbook(String title, List<T> data) throws Exception {
        //创建一个HSSFWorkbook，对应一个Excel文件
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
        //在workbook中添加一个sheet,对应Excel文件中的sheet
        HSSFSheet hssfSheet = this.setSheetStyle(hssfWorkbook,title);
        Map<String,HSSFCellStyle> gridStyle = new HashMap<>();// 表格样式
        List<String> headers = new ArrayList<>();
        for (Field field : this.fields){
            if (field.isAnnotationPresent(ExcelColumn.class)){
                field.setAccessible(true);
                ExcelColumn excelColumn = field.getAnnotation(ExcelColumn.class);
                headers.add(excelColumn.name());
                if (excelColumn.style() && this.gridStyle != null){
                    HSSFCellStyle cellStyle = this.gridStyle.execute(hssfWorkbook, field);
                    cellStyle.setLocked(excelColumn.lock());
                    gridStyle.put(field.getName(),cellStyle);
                }else {
                    gridStyle.put(field.getName(),setCellStyle(excelColumn,hssfWorkbook.createCellStyle(),hssfWorkbook.createFont()));
                }
                if (excelColumn.lock()) {
                    hssfSheet.protectSheet(excelColumn.lockPassword());
                }
            }
        }
        //创建标题合并行
        hssfSheet.addMergedRegion(new CellRangeAddress(0,(short)0,0,(short)headers.size() - 1));
        //产生标题行
        /*HSSFRow hssfRow = hssfSheet.createRow(0);
        HSSFCell cell = hssfRow.createCell(0);
        cell.setCellValue(title);*/
        //设置默认标题样式
        /*if (this.titleStyle == null){
            HSSFCellStyle style = hssfWorkbook.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);   //设置居中样式
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            //设置标题字体
            Font titleFont = hssfWorkbook.createFont();
            titleFont.setFontHeightInPoints((short) 14);
            style.setFont(titleFont);
            cell.setCellStyle(style);
        }else{
            cell.setCellStyle(this.titleStyle.execute(hssfWorkbook));
        }*/

        //设置表头样式
        HSSFCellStyle headerStyle = this.setHeadStyle(hssfWorkbook,hssfSheet);
        //产生表头
        HSSFRow row1 = hssfSheet.createRow(1);
        for (int i = 0, length = headers.size(); i < length; i++){
            HSSFCell hssfCell = row1.createCell(i);
            hssfCell.setCellValue(headers.get(i));
            hssfCell.setCellStyle(headerStyle);
        }
        //创建内容
        for (int i = 0,length = data.size(); i < length; i++){
            row1 = hssfSheet.createRow(i + 2);
            T t = data.get(i);
            int k = 0;
            for (Field field : this.fields){
                if (field.isAnnotationPresent(ExcelColumn.class)){
                    ExcelColumn excelColumn = field.getAnnotation(ExcelColumn.class);
                    if (!excelColumn.autoHeight()){
                        row1.setHeightInPoints(excelColumn.height());
                    }
                    // 设置列宽
                    hssfSheet.setColumnWidth(k,(int)((excelColumn.width() + 0.72) * 256));
                    //将内容按顺序赋给对应列对象
                    HSSFCell hssfCell = row1.createCell(k++);
                    hssfCell.setCellStyle(gridStyle.get(field.getName()));
                    field.setAccessible(true);
                    String defaultValue = excelColumn.defaultValue();
                    Object val = field.get(t);
                    if ((val == null || "".equals(val)) && !"".equals(defaultValue)){
                        val = defaultValue;
                    }
                    if (val != null){
                        String suffix = excelColumn.suffix();
                        String converExp = excelColumn.converExp();
                        Class<?> fieldType = field.getType();
                        if (!"".equals(suffix)){
                            val += suffix;
                        }
                        if (!"".equals(converExp)){
                            hssfCell.setCellValue(convertByExp(String.valueOf(val),converExp));
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
                            if ("".equals(excelColumn.dateformat())){
                                hssfCell.setCellValue(val.toString());
                            }else {
                                hssfCell.setCellValue(new SimpleDateFormat(excelColumn.dateformat()).format(val));
                            }
                        }
                    }
                }
            }
        }
        return hssfWorkbook;
    }

    /**
     * 设置标题样式(可选)
     * param titleStyle
     */
    public void setTitleStyle(Style style){
        this.titleStyle = style;
    }

    /**
     * 设置表头样式(可选)
     * param headerStyle
     */
    public void setHeaderStyle(Style style){
        this.headerStyle = style;
    }

    /**
     * 设置表格样式(可选)
     * param headerStyle
     */
    public void setGridStyle(GridStyle gridStyle){
        this.gridStyle = gridStyle;
    }

    /**
     * 设置sheet样式(可选)
     */
    public HSSFSheet setSheetStyle(HSSFWorkbook hssfWorkbook,String title){
        if (Objects.equals(this.titleStyle,null) && this.clazz.isAnnotationPresent(ExcelSheet.class)){
            ExcelSheet excelSheet = this.clazz.getAnnotation(ExcelSheet.class);
            String titleVal = "".equals(excelSheet.name()) ? title : excelSheet.name();
            HSSFSheet hssfSheet = hssfWorkbook.createSheet(titleVal);
            //产生标题行
            HSSFRow hssfRow = hssfSheet.createRow(0);
            HSSFCell cell = hssfRow.createCell(0);
            cell.setCellValue(titleVal);
            // 标题样式
            HSSFCellStyle style = hssfWorkbook.createCellStyle();
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setAlignment(excelSheet.align());   //设置居中样式
            style.setVerticalAlignment(excelSheet.vertical());
            style.setFillForegroundColor(excelSheet.backColor().getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setWrapText(excelSheet.wrap());
            style.setLocked(excelSheet.lock());
            if (excelSheet.lock()) {
                hssfSheet.protectSheet(excelSheet.lockPassword());
            }
            //设置标题字体
            Font titleFont = hssfWorkbook.createFont();
            titleFont.setFontHeightInPoints(excelSheet.fontSize());
            titleFont.setColor(excelSheet.color().getIndex());
            titleFont.setItalic(excelSheet.italic());
            titleFont.setBold(excelSheet.bold());
            if (!"".equals(excelSheet.fontName())){
                titleFont.setFontName(excelSheet.fontName());
            }
            style.setFont(titleFont);
            cell.setCellStyle(style);
            return hssfSheet;
        }else if (this.titleStyle != null){// 自定义样式
            HSSFSheet hssfSheet = hssfWorkbook.createSheet(title);
            //产生标题行
            HSSFRow hssfRow = hssfSheet.createRow(0);
            HSSFCell cell = hssfRow.createCell(0);
            cell.setCellValue(title);
            cell.setCellStyle(this.titleStyle.execute(hssfWorkbook));
            return hssfSheet;
        }else {// 默认样式
            HSSFSheet hssfSheet = hssfWorkbook.createSheet(title);
            //产生标题行
            HSSFRow hssfRow = hssfSheet.createRow(0);
            HSSFCell cell = hssfRow.createCell(0);
            cell.setCellValue(title);
            HSSFCellStyle style = hssfWorkbook.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);   //设置居中样式
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setLocked(false);
            //设置标题字体
            Font titleFont = hssfWorkbook.createFont();
            titleFont.setFontHeightInPoints((short) 14);
            style.setFont(titleFont);
            cell.setCellStyle(style);
            return hssfSheet;
        }
    }

    /**
     * 设置表头注解样式(可选)
     */
    public HSSFCellStyle setHeadStyle(HSSFWorkbook hssfWorkbook,HSSFSheet hssfSheet){
        if (Objects.equals(this.headerStyle,null) && this.clazz.isAnnotationPresent(ExcelHead.class)){// 注解样式
            ExcelHead excelHead = this.clazz.getAnnotation(ExcelHead.class);
            HSSFCellStyle hssfCellStyle = hssfWorkbook.createCellStyle();
            hssfCellStyle.setBorderBottom(BorderStyle.THIN);
            hssfCellStyle.setBorderLeft(BorderStyle.THIN);
            hssfCellStyle.setBorderRight(BorderStyle.THIN);
            hssfCellStyle.setBorderTop(BorderStyle.THIN);
            hssfCellStyle.setAlignment(excelHead.align());
            hssfCellStyle.setVerticalAlignment(excelHead.vertical());
            hssfCellStyle.setWrapText(excelHead.wrap());
            hssfCellStyle.setFillForegroundColor(excelHead.backColor().getIndex());
            hssfCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hssfCellStyle.setLocked(excelHead.lock());
            if (excelHead.lock()) {
                hssfSheet.protectSheet(excelHead.lockPassword());
            }
            HSSFFont font = hssfWorkbook.createFont();
            font.setFontHeightInPoints(excelHead.fontSize());
            font.setColor(excelHead.color().getIndex());
            font.setItalic(excelHead.italic());
            font.setBold(excelHead.bold());
            if (!"".equals(excelHead.fontName())){
                font.setFontName(excelHead.fontName());
            }
            hssfCellStyle.setFont(font);
            return hssfCellStyle;
        }else if (this.headerStyle != null){// 自定义样式
            return this.headerStyle.execute(hssfWorkbook);
        }else {// 默认样式
            HSSFCellStyle hssfCellStyle = hssfWorkbook.createCellStyle();
            hssfCellStyle.setAlignment(HorizontalAlignment.CENTER);   //设置水平居中样式
            hssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);   //设置上下居中样式
            hssfCellStyle.setBorderBottom(BorderStyle.THIN);
            hssfCellStyle.setBorderLeft(BorderStyle.THIN);
            hssfCellStyle.setBorderRight(BorderStyle.THIN);
            hssfCellStyle.setBorderTop(BorderStyle.THIN);
            hssfCellStyle.setLocked(false);
            return hssfCellStyle;
        }
    }

    /**
     * 创建并设置单元格
     * param excel
     * param style
     */
    private HSSFCellStyle setCellStyle(ExcelColumn excelColumn, HSSFCellStyle style, Font font){
        // 创建锁定的单元格
        //HSSFCellStyle style2 = hssfWorkbook.createCellStyle();
        style.setLocked(excelColumn.lock());
        style.setAlignment(excelColumn.align());//设置水平居中样式
        style.setVerticalAlignment(excelColumn.vertical());// 设置垂直居中
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setWrapText(excelColumn.wrap());// 是否自动换行
        font.setFontHeightInPoints(excelColumn.fontSize());
        //font.setColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex()); //字体颜色
        font.setColor(excelColumn.color().getIndex()); //字体颜色
        font.setItalic(excelColumn.italic());
        font.setBold(excelColumn.bold());
        if (!"".equals(excelColumn.fontName())){
            font.setFontName(excelColumn.fontName());
        }
        style.setFont(font);
        //设置单元格颜色（颜色对应枚举会放在下面）
        //style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        //style.setFillForegroundColor(IndexedColors.AQUA.getIndex());
        style.setFillForegroundColor(excelColumn.backColor().getIndex());
        //全部填充 （填充枚举对应的样式也会放在下面）
        //style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * 解析导出值 0=男,1=女,2=未知
     * param propertyValue 参数值
     * param converterExp 翻译注解
     */
    private String convertByExp(String propertyValue, String converExp){
        String[] convertSource = converExp.split(",");
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
    private String reverseByExp(String propertyValue, String converExp){
        String[] convertSource = converExp.split(",");
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

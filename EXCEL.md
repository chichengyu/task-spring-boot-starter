## Excel 工具使用
Excel的工具类，方便导入与导出,提供三个注解

 + ExcelSheet(用于设置标题样式与名称及sheet名称,不需要也可不设置)
 + ExcelHead(用于设置表头列样式,不需要也可不设置)
 + ExcelColumn(用于设置表格每列样式,此注解` name `(列名称)属性必须设置)

##### 使用说明
如果单纯使用 ` Excel `工具, 导包
```
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>2.1.1.RELEASE</version>
    <!-- 排除多余 quartz  -->
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
如果只是使用` 2.1.1.RELEASE ` 版本的 ` task定时任务`，可以参考下面导包坐标(排除多余依赖)，也可以直接使用之前版本[1.3.3.RELEASE](https://github.com/chichengyu/task-spring-boot-starter)(`只包含定时任务依赖`)，虽然不排除也没什么影响，但可以使项目体量小一些，导包
```
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>2.1.1.RELEASE</version>
    <!-- 排除多余 excel -->
    <exclusions>
        <exclusion>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
使用方式，参考下面，两种使用方式：
 + 方式1:[使用` Excel `注解](#Excel注解),(也适用于随意组合数据,只是需要创建对应的vo类,然后加注解)
 + 方式2:[不使用注解(扩展性强,适用于随意组合的集合数据)](#不使用注解),无需创建对应的vo类

### [Excel注解](#使用说明)
首先在实体上加上 ` Excel ` 注解(还有很多属性,可自行查看注解里说明)，如：实体 ` TestPojo `
```
//@ExcelSheet(fontSize = 22,color = IndexedColors.DARK_YELLOW,backColor = IndexedColors.SKY_BLUE,bold = true,fontName = "微软雅黑",lock = true)
//@ExcelHead(backColor = IndexedColors.GREEN,color = IndexedColors.YELLOW,fontSize = 18,wrap = true,lock = true,bold = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_test")
public class TestPojo implements Serializable {
    private static final long serialVersionUID = -169520293430625480L;

    @ExcelColumn(name = "ID",height = 32,autoHeight = false,style = true,lock = true)
    private Integer id;

    @ExcelColumn(name = "名称",width = 32,suffix = "(单位%)",wrap = true,backColor = IndexedColors.DARK_GREEN,color = IndexedColors.BRIGHT_GREEN)
    private String name;

    @ExcelColumn(name = "创建日期",width = 20,dateformat = "yyyy-MM-dd HH:mm:ss",lock = true,bold = true)
    private Date createTime;

    @ExcelColumn(name = "年龄",handler = AgeHandler.class,converExp = "0=男,1=女,2=未知",fontSize = 16,fontName = "微软雅黑",style = true,lock = true)
    private Integer age;

    @ExcelColumn(name = "测试数字默认值")
    private Integer num;

    @ExcelColumn(name = "测试数字默认值1",converExp = "100=差,200=良好,300=优秀")
    private Integer aa;

    @ExcelColumn(name = "测试数字默认值2",converExp = "100=差,200=良好,300=优秀")
    private Long bb;

    @ExcelColumn(name = "测试数字默认值3",converExp = "100.00=差,200.00=良好,300.00=优秀")
    private Float cc;

    @ExcelColumn(name = "测试数字默认值4",readDefaultValue = true, converExp = "100.00=差,200.00=良好,300.00=优秀")
    private Double dd;
}
```
在接口中使用
```
@Controller
@RequestMapping("/excel")
public class ExcelTestController extends BaseController{

    /**
     * 导入excel测试
     * @return
     */
    @Log(module = "Excel",action = "导入Excel")
    @PostMapping("/import")
    @ResponseBody
    public R<List<TestPojo>> excelImport(MultipartFile file){
        try{
            //excel文件名
            Excel<TestPojo> excel = new Excel<>(TestPojo.class);
            // 打印error信息
            excel.setError(error -> System.out.println(error));
            //List<TestPojo> pojoList = excel.read("d:/test.xls");
            InputStream inputStream = file.getInputStream();
            List<TestPojo> pojoList = excel.read(inputStream);
            // 或者使用静态方法
            //Excel.type(TestPojo.class).read(file.getInputStream());
            return R.ok(pojoList);
        }catch (Exception e){
            return R.error(e.getMessage());
        }
    }

    /**
     * 导出excel测试
     * @return
     */
    @Log(module = "Excel",action = "导出Excel")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        //获取数据
        List<TestPojo> list = new ArrayList<>();
        Date date = new Date();
        for (int i = 0; i < 10; i++) {
            TestPojo testPojo = new TestPojo();
            // 空对象导出时为导入模板（只有表头）
            /*testPojo.setId(i);
            testPojo.setName("小池" + i);
            testPojo.setAge(RandomUtils.nextInt(0,2));
            testPojo.setCreateTime(date);
            list.add(testPojo);*/
        }
        //excel文件名
        //Excel<TestPojo> excel = new Excel<>(TestPojo.class);
        //excel.export(response,"用户信息表",list);
        // 也可以直接使用静态方法
        //Excel.type(TestPojo.class).export(response,"用户信息表",list);
        Excel<TestPojo> excel = new Excel<>(TestPojo.class);
        // 打印error信息
        excel.setError(error -> System.out.println(error));
        excel.export(response, "1111", data);
    }
}
```
注：如果想在导出或者导入的时候处理数据,可以实现` 数据处理器接口ExcelHandlerAdapter `并配置注解`@ExcelColumn(handler = AgeHandler.class)`且实现方法` format `方法，然后编写如下代码:
```
/**
 * author chics
 * 2023/11/7
 */
public class AgeHandler implements ExcelHandlerAdapter {

    @Override
    public Object format(Object value,byte[] fileStream) {
        System.out.println("------年龄数据处理器执行了，参数"+value);
        return 100;
    }
}
```
即可执行处理数据。
##### 文件处理(包括图片)
实体文件的字段(也就是这注解` @ExcelColumn `标记的字段),一般设置为`string`字符串,因为路径字段就是字符串。
```
@ExcelColumn(name = "图片",handler = ImgHandler.class,cellType = ExcelColumn.ColumnType.FILE)
private String img;
```

###### 导出
导出很简单，在文件路径字段上,配置注解` @ExcelColumn(name = "图片",cellType = ExcelColumn.ColumnType.FILE) `,即可完成，然后在导出的数据里设置文件路径字段(注：路径可以是本地路径,也可以是网络图片路径,总之路径能正常访问到文件即可)，即可实现导出图片到excel。

###### 导入
导入也很简单，首先，配置注解` @ExcelColumn(name = "图片",handler = ImgHandler.class,cellType = ExcelColumn.ColumnType.FILE) `,然后创建图片数据处理器` ImgHandler.java `并实现接口` ExcelHandlerAdapter `,且实现方法` format `方法，如下：
```
/**
 * author chics
 * 2023/11/7
 */
public class ImgHandler implements ExcelHandlerAdapter {

    @Override
    public Object format(Object value,byte[] fileStream) {
        System.out.println("------图片数据处理器执行了------");
        String fileName = this.getImageFileName(fileStream);
        String path = "d:\\f\\"+fileName;
        try {
            // 这里我是保存到本地了，此处可以上传到oss ...
            this.writeBytesToFile(fileStream,path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "http://baidu.com/"+path;
    }
}
```
即可完成图片导入。

##### 自定义样式(自定义样式与注解ExcelSheet/ExcelHead,可不设置,有默认样式,优先级:自定义 > 注解 > 默认)
```
//excel文件名
Excel<TestPojo> excel = new Excel<>(TestPojo.class);
// 此时可以设置表头样式（不设置也可以）
excel.setHeaderStyle(workbook -> {
    CellStyle headerStyle = workbook.createCellStyle();
    headerStyle.setAlignment(HorizontalAlignment.CENTER);   //设置水平居中样式
    headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);   //设置上下居中样式
    headerStyle.setBorderBottom(BorderStyle.THIN);
    headerStyle.setBorderLeft(BorderStyle.THIN);
    headerStyle.setBorderRight(BorderStyle.THIN);
    headerStyle.setBorderTop(BorderStyle.THIN);
    // 如果设置背景色 BackgroundColor 无效，必须调用 ForegroundColor,且必须在调用 setFillPattern 才能生效
    headerStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return headerStyle;
});
// 此时可以设置标题样式（不设置也可以）
excel.setTitleStyle(workbook -> {
    CellStyle style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.CENTER);   //设置水平居中样式
    style.setVerticalAlignment(VerticalAlignment.CENTER);   //设置上下居中样式
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    // 如果设置背景色 BackgroundColor 无效，必须调用 ForegroundColor,且必须在调用 setFillPattern 才能生效
    style.setFillForegroundColor(IndexedColors.RED.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    //设置标题字体
    Font titleFont = workbook.createFont();
    titleFont.setFontHeightInPoints((short) 14);
    titleFont.setColor(IndexedColors.DARK_YELLOW.getIndex());
    titleFont.setBold(true);
    style.setFont(titleFont);
    return style;
});
// 此时可以设置自定义表格列样式（不设置也可以）,使扩展性更强了,需要什么样式可自行定义
excel.setGridStyle((workbook,field) -> {
    CellStyle cellStyle = workbook.createCellStyle();
    cellStyle.setAlignment(HorizontalAlignment.CENTER);   //设置水平居中样式
    cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);   //设置上下居中样式
    cellStyle.setBorderBottom(BorderStyle.THIN);
    cellStyle.setBorderLeft(BorderStyle.THIN);
    cellStyle.setBorderRight(BorderStyle.THIN);
    cellStyle.setBorderTop(BorderStyle.THIN);
    //设置字体
    Font font = workbook.createFont();
    font.setFontHeightInPoints((short) 12);
    font.setColor(IndexedColors.DARK_YELLOW.getIndex());
    // 可以根据列的不同,设置不同的列样式,但需要在注解里开启自定义样式,如：@Excel(name = "ID",style = true)
    // @Excel(name = "ID",style = true)
    if ("id".equals(field.getName())){
        font.setBold(true);
    }
    // @Excel(name = "年龄",style = true)
    if ("age".equals(field.getName())){
        font.setColor(IndexedColors.RED.getIndex());
        // 如果设置背景色 BackgroundColor 无效，必须调用 ForegroundColor,且必须在调用 setFillPattern 才能生效
        cellStyle.setFillForegroundColor(IndexedColors.BLUE1.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }
    cellStyle.setFont(font);
    return cellStyle;
});
excel.export(response,"用户信息表",list);
```

### [不使用注解](#使用说明)
直接在接口中使用 
```
@Controller
@RequestMapping("/excel")
public class ExcelTestController extends BaseController{
    // ------------------------------------ 不使用Excel注解 -----------------------------------------------
    /**
     * 导入excel测试
     * @return
     */
    @Log(module = "Excel",action = "导入Excel")
    @PostMapping("/import2")
    @ResponseBody
    public R<List<Map<String, Object>>> excelImport2(MultipartFile file){
        try{
            // 与 String[] headers = {"用户ID", "用户名称", "用户密码", "用户手机","创建时间"}; 一一对应
            String[] fields = {"id","name","password","time"};
            InputStream inputStream = file.getInputStream();
            List<Map<String, Object>> mapList = Excel.read2(inputStream, Arrays.asList(fields));
            return R.ok(mapList);
        }catch (Exception e){
            return R.error(e.getMessage());
        }
    }

    /**
     * 导出 excel
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/export2")
    @ResponseBody
    public void export2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //获取数据
        List<TestPojo> list = new ArrayList<>();
        Date date = new Date();
        for (int i = 0; i < 10; i++) {
            TestPojo testPojo = new TestPojo();
            testPojo.setId(i);
            testPojo.setName("小池" + i);
            testPojo.setAge(RandomUtils.nextInt(0,2));
            testPojo.setCreateTime(date);
            list.add(testPojo);
        }

        //excel标题
        String[] headers = {"用户ID", "用户名称", "用户密码", "创建时间"};

        //excel sheet名
        String fileName = "用户信息表";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String [][] content = new String[list.size()][5];
        for (int i = 0; i < list.size(); i++) {
            content[i] = new String[headers.length];
            TestPojo obj = list.get(i);
            content[i][0] = obj.getId().toString();
            content[i][1] = obj.getName();
            content[i][2] = obj.getAge().toString();
            content[i][3] = dateFormat.format(obj.getCreateTime());
        }
        Excel.export2(response,fileName,headers,content);
    }
}
```
到此完成。

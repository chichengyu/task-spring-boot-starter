## Excel 工具使用
Excel的工具类，方便导入与导出

##### 使用说明
如果单纯使用 ` Excel `工具, 导包
```
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>1.3.6.RELEASE</version>
    <!-- 排除多余 quartz  -->
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
如果只是使用` 1.3.6.RELEASE ` 版本的 ` task任务`，可以参考下面导包坐标(排除多余依赖)，可以直接使用之前版本[1.3.3.RELEASE](https://github.com/chichengyu/task-spring-boot-starter)，虽然不排除也没什么影响，但可以使项目体量小一些，导包
```
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>1.3.6.RELEASE</version>
    <!-- 排除多余 tomcat / excel -->
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
        <exclusion>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
使用方式，参考下面，两种使用方式：
 + [使用` Excel `注解](#Excel注解)
 + [不使用注解(扩展性强,适用于随意组合的集合数据)](#不使用注解)

### [Excel注解](#使用说明)
首先在实体上加上 ` Excel ` 注解，如：实体 ` TestPojo `
```
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_test")
public class TestPojo implements Serializable {
    private static final long serialVersionUID = -169520293430625480L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Excel(name = "ID")
    private Integer id;

    @Excel(name = "名称",suffix = "(单位%)")
    private String name;

    @Excel(name = "创建日期",width = 20,dateformat = "yyyy-MM-dd HH:mm:ss")
    @Transient
    private Date createTime;

    @Excel(name = "年龄",readConverterExp = "0=男,1=女,2=未知")
    @Transient
    private Integer age;

    @Excel(name = "测试数字默认值")
    @Transient
    private Integer num;

    @Excel(name = "测试数字默认值1",readConverterExp = "100=差,200=良好,300=优秀")
    @Transient
    private Integer aa;

    @Excel(name = "测试数字默认值2",readConverterExp = "100=差,200=良好,300=优秀")
    @Transient
    private Long bb;

    @Excel(name = "测试数字默认值3",readConverterExp = "100.00=差,200.00=良好,300.00=优秀")
    @Transient
    private Float cc;

    @Excel(name = "测试数字默认值4",readCoverDefaultValue = true, readConverterExp = "100.00=差,200.00=良好,300.00=优秀")
    @Transient
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
    @Login(isLogin = false)
    @PostMapping("/import")
    @ResponseBody
    public R<List<TestPojo>> excelImport(MultipartFile file){
        try{
            //excel文件名
            ExcelUtil<TestPojo> excelUtil = new ExcelUtil<>(TestPojo.class);
            //List<TestPojo> pojoList = excelUtil.read("d:/test.xls");
            InputStream inputStream = file.getInputStream();
            List<TestPojo> pojoList = excelUtil.read(inputStream);
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
        ExcelUtil<TestPojo> excelUtil = new ExcelUtil<>(TestPojo.class);
        excelUtil.export(response,"用户信息表",list);
    }
}
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
    @Login(isLogin = false)
    @PostMapping("/import2")
    @ResponseBody
    public R<List<Map<String, Object>>> excelImport2(MultipartFile file){
        try{
            // 与 String[] headers = {"用户ID", "用户名称", "用户密码", "用户手机","创建时间"}; 一一对应
            String[] fields = {"id","name","password","time"};
            InputStream inputStream = file.getInputStream();
            List<Map<String, Object>> mapList = ExcelUtil.read2(inputStream, Arrays.asList(fields));
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
        ExcelUtil.export2(response,fileName,headers,content);
    }
}
```
到此完成。

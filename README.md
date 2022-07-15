# job-spring-boot-starter

#### 介绍
自定义spring定时任务starter

#### 安装
下载项目,cmd到项目根目录，打成jar，安装命令
```
mvn clean install

```
如果下载了本地项目，执行如上安装命令后就无需执行后面命令；如果没有项目是jar包文件，那么执行命令进行安装到maven仓库
```
mvn install:install-file -Dfile=D:\job-spring-boot-starter-1.0.RELEASE.jar -DgroupId=org.job -DartifactId=job-spring-boot-starter -Dversion=1.0.RELEASE -Dpackaging=jar
```
在这段命令中，` -Dfile `参数指你自定义JAR包文件所在的路径，并依次指定了自定义的` GroupId `、` ArtifactId `和` Version `信息。 

#### 使用说明

使用很简单，项目里引入坐标
```
<dependency>
    <groupId>com.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>1.0.RELEASE</version>
</dependency>
```
创建一个配置文件 ` TaskConfig.java `
```
@Configuration
public class TaskConfig {

    @Autowired
    private JobLogDao jobLogDao;// 需自行实现

    @Bean
    public TaskManager taskManager(ApplicationContext applicationContext){
        TaskManager taskManager = new TaskManager(jobLog -> jobLogDao.save(jobLog));// 保存定时任务日志到数据库中
        taskManager.init(applicationContext);// 初始化，注入上下文
        return taskManager;
    }
}
```
创建任务Bean类  ` TestTask `
```
@Component("testTask")
public class TestTask implements ITask<String> {

    @Override
    public R<String> run(String params) {
        System.out.println("测试定时任务执行了，参数:"+params);
        return R.ok();
    }
}
``` 
然后创建一个控制器 ` TestController `
```
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private TaskManager taskManager;

    @GetMapping("/task/add")
    public String add() {
        JobTask jobTask = new JobTask();
        jobTask.setBeanName("testTask");// TestTask 类的 BeanName名称
        jobTask.setJobId(100L);
        jobTask.setStatus(0);// 0成功1失败
        jobTask.setParams("我是测试定时器");
        jobTask.setCronExpression("*/2 * * * * ?");
        jobTask.setRemark("测试");
        jobTask.setCreateTime(new Date());
        taskManager.addCronTask(jobTask);
        return "ok";
    }
}
```
然后启动项目，访问接口，即可看到定时任务执行。
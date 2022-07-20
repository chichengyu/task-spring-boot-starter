# task-spring-boot-starter

#### 介绍
自定义spring定时任务starter，sql可执行创建表

#### 使用说明

使用很简单，项目里引入坐标
```
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>1.2.6.RELEASE</version>
</dependency>
```
创建一个配置文件 ` TaskConfig.java `
```
@Configuration
public class TaskConfig {

    @Autowired
    private JobLogDao jobLogDao;// 需自行实现

    @Bean
    public TaskManager taskManager(){
        // 不需要记录日志到数据库
        TaskManager taskManager = new TaskManager();
        taskManager.setPoolSize(5);
        taskManager.setPrefix("task_");
        taskManager.setErrorHandler(e -> {
            log.error("执行异常：{}",e);
            // 可以给管理者发送邮件 ...
        });
        taskManager.init();// 初始化

        // 需要记录日志到数据库
        /*TaskManager taskManager = new TaskManager(jobLog -> jobLogDao.save(jobLog));// 保存定时任务日志到数据库中
        taskManager.setPoolSize(5);
        taskManager.setPrefix("task_");
        taskManager.setErrorHandler(e -> {
            log.error("执行异常：{}",e);
            // 可以给管理者发送邮件 ...
        });
        taskManager.init();// 初始化
        return taskManager;*/
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
        jobTask.setStatus(0);// 0正常1禁止   JobTaskLog 日志的 status 0代表定时任务运行结果成功 1失败
        jobTask.setParams("我是测试定时器");
        jobTask.setCronExpression("*/2 * * * * ?");
        jobTask.setRemark("测试");
        jobTask.setCreateTime(new Date());
        taskManager.addCronTask(jobTask);
        // taskManager.updateCronTask(jobTask);// 更新任务
        // taskManager.runNow(jobTask);// 立即执行
        // taskManager.cancel(jobTask.getJobId());// 取消任务
        // taskManager.refresh();// 批量添加任务
        // 正在运行的任务
        // ConcurrentHashMap<Long, TaskManager.ScheduledRealTaskFuture> taskContainer = taskManager.getTaskContainer();
        return "ok";
    }
}
```
然后启动项目，访问接口，即可看到定时任务执行。

# task-spring-boot-starter
<p align="left">
    <a href="https://github.com/chichengyu/task-spring-boot-starter">
        <img src="https://img.shields.io/badge/%E4%BD%9C%E8%80%85-%E5%B0%8F%E6%B1%A0-%23129e50" alt="MIT License" />
    </a>
    <a href="https://github.com/chichengyu/task-spring-boot-starter">
        <img src="https://img.shields.io/badge/version-1.2.7.RELEASE-blue" alt="version-1.2.7.RELEASE" />
    </a>
    <a href="https://github.com/chichengyu/task-spring-boot-starter">
        <img src="https://img.shields.io/badge/version-1.3.3.RELEASE-orange" alt="version-1.3.3.RELEASE" />
    </a>
    <a href="https://github.com/chichengyu/task-spring-boot-starter">
        <img src="https://img.shields.io/badge/last version-2.2.0.RELEASE-green" alt="version-2.2.0.RELEASE" />
    </a>
</p>

## 介绍
自定义spring定时任务starter，sql可执行创建表。有两种使用方式：   
 - :heavy_check_mark: 方式1: [spring中的 TaskScheduler](#spring中的TaskScheduler) ` (持久化时执行 sql/job.sql) `
 - :heavy_check_mark: 方式2: [Quartz 任务调度框架](#Quartz任务调度框架) ` (持久化时执行 sql/quartz.sql )`
 - 其他工具类：[Excel工具类(2.2.0.RELEASE版本集成)](https://github.com/chichengyu/task-spring-boot-starter/blob/main/EXCEL.md)

:warning:注：sql文件 ` job_task 任务表`/ ` job_task_log 任务日志表`，需要持久化的可以创建表，在添加任务的同时插入表中。


如果要查看任务` debug `日志，需要在项目配置文件` application.yml `配置
```
logging:
  level:
    com.job.task: debug
```
重启项目，可以看到控制台任务` debug `日志已经打印了。:rocket::rocket::rocket::rocket::rocket::rocket:

#### 使用说明
:lollipop:使用很简单，项目里引入坐标，`1.2.7.RELEASE `(使用spring自带的 ` TaskScheduler `,没有集成 ` Quartz `)
```
<!-- 使用spring中的 TaskScheduler -->
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>1.2.7.RELEASE</version>
</dependency>
```
` 1.3.3.RELEASE ` 开始集成 ` Quartz `，也支持:heart:[`spring自带的 TaskScheduler(使用时排除Quartz依赖`)](#spring中的TaskScheduler):heart:  
```
<!-- 1.3.3.RELEASE开始集成 Quartz -->
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>1.3.3.RELEASE</version>
</dependency>

<!-- 2.2.0.RELEASE集成 Quartz（只使用定时器，排除excel） -->
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>2.2.0.RELEASE</version>
    <!-- 排除多余 excel -->
    <exclusions>
        <exclusion>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
第1步:point_right:：创建任务Bean类  ` TestTask `,需要实现接口 ` com.job.task.ITask<string> `,多个定时器(`创建多个任务Bean类实现接口ITask<string>`)
```
@Component("testTask")
public class TestTask implements ITask<String> {

    @Override
    public R<String> run(String params) {
        System.out.println("测试定时任务执行了，参数:"+params);
        try {
            JobTask jobTask = JSON.parseObject(params, JobTask.class);
            System.out.println(jobTask.toString());
            // 逻辑处理... 
            return R.ok();
        } catch (Exception e) {
            // 会将异常信息记录到 JobTaskLog 的 message 成员属性中
            return R.error(e.toString());
        }
    }
}
``` 
第2步:point_right:：就是创建配置文件` TaskConfig `/` TaskQuartzConfig `与任务管理` TaskManager `/` TaskQuartzManager `，持久化都需要添加任务时插入 ` job_task 任务表 `/ ` job_task_log 任务日志表 `,只是不同的是 `Quartz`还有自己的表(不需要我们管)，我们只需要关心` job_task 任务表 `/ ` job_task_log 任务日志表 `，需要我们记录到数据库。:baby_chick::baby_chick::baby_chick::baby_chick::baby_chick::baby_chick:

第3步:point_right:：就是参考下面2个使用方式，分别创建对应的配置文件` TaskConfig `/` TaskQuartzConfig `与任务管理` TaskManager `/` TaskQuartzManager `，进行增删改查。
### [spring中的TaskScheduler](#使用说明)
:pushpin: 引入依赖,:lollipop:排除多余的 ` Quartz 依赖`,:lollipop:也可以引入之前的 ` 1.2.7.RELEASE `版本
```
<!-- 1.3.3.RELEASE开始集成 Quartz（2.2.0.RELEASE也如此引入） -->
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>1.3.3.RELEASE</version>
    <!-- 方式一(spring中的TaskScheduler)，排除 Quartz -->
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 2.2.0.RELEASE -->
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>2.2.0.RELEASE</version>
    <!-- 方式一(spring中的TaskScheduler)，排除 Quartz 与 excel -->
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </exclusion>
        <exclusion>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
:zap::zap::zap: 一定要排除`Quartz依赖` :zap::zap::zap:,虽然不影响，但总归使用项目更简洁:heart::heart::heart:  
:lollipop:创建一个配置文件 ` TaskConfig.java `
```
@Configuration
public class TaskConfig {

    @Autowired
    private JobTaskLogDao jobTaskLogDao;// 保存到数据库,需自行实现，提供的 sql/JobTaskLogDao 表

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
        /*TaskManager taskManager = new TaskManager(jobLog -> jobTaskLogDao.save(jobLog));// 保存定时任务日志到数据库中
        taskManager.setPoolSize(5);
        taskManager.setPrefix("task_");
        taskManager.setErrorHandler(e -> {
            log.error("执行异常：{}",e);
            // 可以给管理者发送邮件 ...
        });
        //taskManager.setJobTaskLogSave(jobLog -> jobTaskLogDao.save(jobLog));//也可以这样设置
        taskManager.init();// 初始化 */
        return taskManager;
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
    @Autowired
    private JobTaskDao jobTaskDao;

    @GetMapping("/task/add")
    public String add() {
        JobTask jobTask = new JobTask();
        jobTask.setBeanName("testTask");// TestTask 类的 BeanName名称
        jobTask.setJobId(100L);
        jobTask.setStatus(0);// 0正常1禁止   JobTaskLog 日志的 status 0代表定时任务运行结果成功 1失败
        jobTask.setParams("我是测试定时器");
        jobTask.setCron("*/2 * * * * ?");
        jobTask.setRemark("测试");
        jobTask.setCreateTime(new Date());
        jobTaskDao.save(jobTask);// 同时持久化保存到自定义的 job_task 表中
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
然后启动项目，访问接口，即可看到定时任务执行。:dango::dango::dango::dango::dango::dango::dango::dango::dango::dango::dango::dango:

### [Quartz任务调度框架](#使用说明)
引包
```
<!-- 2.2.0.RELEASE -->
<dependency>
    <groupId>io.github.chichengyu</groupId>
    <artifactId>task-spring-boot-starter</artifactId>
    <version>2.2.0.RELEASE</version>
    <!-- 方式二(Quartz)，排除 excel -->
    <exclusions>
        <exclusion>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
:pushpin: 创建一个配置文件 ` TaskQuartzConfig.java `
```
@Slf4j
@Configuration
public class TaskQuartzConfig {

    @Autowired
    private JobTaskLogDao jobTaskLogDao;// 保存到数据库,需自行实现，提供的 sql/JobTaskLogDao 表

    /**
     * 必须创建一个 SchedulerFactoryBean 加入到spring容器
     * Quartz 的持久化配置也可以在处配置
     * @return
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(){
    //public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource){
        // 默认内存方式 org.quartz.simpl.RAMJobStore
        SchedulerFactoryBean factoryBean = TaskQuartzManager.getSchedulerFactoryBean();
        /* 可选，默认在 TaskQuartzManager.getSchedulerFactoryBean() 中已经进行配置了
        Properties prop = new Properties();
        prop.put("org.quartz.scheduler.instanceName", "TaskScheduler");// 可自定义
        prop.put("org.quartz.scheduler.instanceId", "AUTO");// 可自定义
        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");// 可自定义
        prop.put("org.quartz.threadPool.threadCount", "20");// 可自定义线程
        prop.put("org.quartz.threadPool.threadPriority", "5");// 可自定义
        factoryBean.setQuartzProperties(prop);*/
        factoryBean.setStartupDelay(5);// 项目启动后5s后开始执行任务

        /*// 这是笔者项目中使用的持久化配置
        //SchedulerFactoryBean factory = TaskQuartzManager.getSchedulerFactoryBean();//也可进行覆盖设置
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);
        //quartz参数
        Properties prop = new Properties();
        prop.put("org.quartz.scheduler.instanceName", "TaskScheduler");
        prop.put("org.quartz.scheduler.instanceId", "AUTO");
        //线程池配置
        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        prop.put("org.quartz.threadPool.threadCount", "20");
        prop.put("org.quartz.threadPool.threadPriority", "5");
        //JobStore配置
        //quartz版本不同可能此配置也不同，如启动报错(就是此配置原因)：org.quartz.SchedulerConfigException: DataSource name not set
        //prop.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        prop.put("org.quartz.jobStore.class", "org.springframework.scheduling.quartz.LocalDataSourceJobStore");
        //集群配置
        prop.put("org.quartz.jobStore.isClustered", "false");// 集群时一定要设置为 true
        prop.put("org.quartz.jobStore.clusterCheckinInterval", "15000");
        prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", "1");

        prop.put("org.quartz.jobStore.misfireThreshold", "12000");
        prop.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
        prop.put("org.quartz.jobStore.selectWithLockSQL", "SELECT * FROM {0}LOCKS UPDLOCK WHERE LOCK_NAME = ?");

        //PostgreSQL数据库，需要打开此注释
        //prop.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");

        factory.setQuartzProperties(prop);
        factory.setSchedulerName("TaskScheduler");
        //延时启动
        factory.setStartupDelay(30);
        factory.setApplicationContextSchedulerContextKey("applicationContextKey");
        //可选，QuartzScheduler 启动时更新己存在的Job，这样就不用每次修改targetObject后删除qrtz_job_details表对应记录了
        factory.setOverwriteExistingJobs(true);
        //设置自动启动，默认为true
        factory.setAutoStartup(true);*/

        return factoryBean;
    }

    /**
     * 创建一个 TaskQuartzManager 用于管理任务
     * @return
     */
    @Bean
    public TaskQuartzManager taskQuartzManager(SchedulerFactoryBean schedulerFactoryBean){
        // 把任务日志保存到数据库
        //TaskQuartzManager taskQuartzManager = new TaskQuartzManager(jobTaskLog -> jobTaskLogDao.save(jobTaskLog));
        TaskQuartzManager taskQuartzManager = new TaskQuartzManager();
        taskQuartzManager.setSchedulerFactoryBean(schedulerFactoryBean);
        taskQuartzManager.setJobTaskLogSave(jobTaskLog -> log.info("日志，[{}]",jobTaskLog));// 此处可以把任务日志保存到数据库
        //taskQuartzManager.setJobNamePrefix("aaaa_");// 可设置任务名称前缀
        taskQuartzManager.init();// 初始化
        return taskQuartzManager;
    }
}
```
然后创建一个控制器 ` TestController `添加任务与上面一样。
```
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private TaskQuartzManager taskQuartzManager;
    @Autowired
    private JobTaskDao jobTaskDao;

    @GetMapping("/task/add")
    public String add() {
        JobTask jobTask = new JobTask();
        jobTask.setBeanName("testTask");// TestTask 类的 BeanName名称
        jobTask.setJobId(100L);
        jobTask.setStatus(0);// 0正常1禁止   JobTaskLog 日志的 status 0代表定时任务运行结果成功 1失败
        jobTask.setParams("我是测试定时器");
        jobTask.setCron("*/2 * * * * ?");
        jobTask.setRemark("测试");
        jobTask.setCreateTime(new Date());
        taskQuartzManager.addCronTask(jobTask);
        jobTaskDao.save(jobTask);// 同时持久化保存到自定义的 job_task 表中
        return "ok";
    }
}
```
启动项目，可以看到定时任务启动了。:heart_eyes::heart_eyes::heart_eyes::heart_eyes::heart_eyes::heart_eyes::heart_eyes::heart_eyes::heart_eyes::heart_eyes::heart_eyes::heart_eyes::heart_eyes::heart_eyes::heart_eyes:

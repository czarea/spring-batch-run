## 1.简介
Spring Batch是一个轻量级，可扩展且全面的批处理框架，可以大规模处理数据。Spring Batch构建在spring框架之上，为执行批处理应用程序提供直观，简单的配置。Spring Batch提供了可处理大量记录所必需的可重用功能，包括日志记录/跟踪，事务管理，作业处理统计，作业重启，跳过和资源管理等交叉问题。

详细介绍可参考：[http://czarea.com/2019/06/10/Spring%20Batch%E7%AE%80%E4%BB%8B/](http://czarea.com/2019/06/10/Spring%20Batch%E7%AE%80%E4%BB%8B/)

本文代码地址：[https://github.com/czarea/spring-batch-run.git](https://github.com/czarea/spring-batch-run.git)

## 2.Hello World

```
plugins {
    id 'org.springframework.boot' version '2.1.6.RELEASE'
    id 'java'
}

apply plugin: 'io.spring.dependency-management'

group = 'com.czarea'
version = '1.0'
sourceCompatibility = '1.8'

repositories {
    mavenCentral()
}

bootJar {
    mainClassName='org.springframework.batch.core.launch.support.CommandLineJobRunner'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-batch'
    runtimeOnly 'org.hsqldb:hsqldb'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.batch:spring-batch-test'
}
```

- 主要配置bootJar的mainClass为CommandLineJobRunner
- 关闭spring batch job 自动启动

```
spring:
  batch:
    job:
      enabled: false
```

**CommandLineJobRunner部分源码：**

```
int start(String jobPath, String jobIdentifier, String[] parameters, Set<String> opts) {

	ConfigurableApplicationContext context = null;

	try {
		try {
			context = new AnnotationConfigApplicationContext(Class.forName(jobPath));
		} catch (ClassNotFoundException cnfe) {
			context = new ClassPathXmlApplicationContext(jobPath);
		}

		context.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

		Assert.state(launcher != null, "A JobLauncher must be provided.  Please add one to the configuration.");
		if (opts.contains("-restart") || opts.contains("-next")) {
			Assert.state(jobExplorer != null,
					"A JobExplorer must be provided for a restart or start next operation.  Please add one to the configuration.");
		}

		String jobName = jobIdentifier;
		
		JobParameters jobParameters = jobParametersConverter.getJobParameters(StringUtils
				.splitArrayElementsIntoProperties(parameters, "="));
		Assert.isTrue(parameters == null || parameters.length == 0 || !jobParameters.isEmpty(),
				"Invalid JobParameters " + Arrays.asList(parameters)
				+ ". If parameters are provided they should be in the form name=value (no whitespace).");

		if (opts.contains("-stop")) {
			List<JobExecution> jobExecutions = getRunningJobExecutions(jobIdentifier);
			if (jobExecutions == null) {
				throw new JobExecutionNotRunningException("No running execution found for job=" + jobIdentifier);
			}
			for (JobExecution jobExecution : jobExecutions) {
				jobExecution.setStatus(BatchStatus.STOPPING);
				jobRepository.update(jobExecution);
			}
			return exitCodeMapper.intValue(ExitStatus.COMPLETED.getExitCode());
		}

		if (opts.contains("-abandon")) {
			List<JobExecution> jobExecutions = getStoppedJobExecutions(jobIdentifier);
			if (jobExecutions == null) {
				throw new JobExecutionNotStoppedException("No stopped execution found for job=" + jobIdentifier);
			}
			for (JobExecution jobExecution : jobExecutions) {
				jobExecution.setStatus(BatchStatus.ABANDONED);
				jobRepository.update(jobExecution);
			}
			return exitCodeMapper.intValue(ExitStatus.COMPLETED.getExitCode());
		}

		if (opts.contains("-restart")) {
			JobExecution jobExecution = getLastFailedJobExecution(jobIdentifier);
			if (jobExecution == null) {
				throw new JobExecutionNotFailedException("No failed or stopped execution found for job="
						+ jobIdentifier);
			}
			jobParameters = jobExecution.getJobParameters();
			jobName = jobExecution.getJobInstance().getJobName();
		}

		Job job = null;
		if (jobLocator != null) {
			try {
				job = jobLocator.getJob(jobName);
			} catch (NoSuchJobException e) {
			}
		}
		if (job == null) {
			job = (Job) context.getBean(jobName);
		}

		if (opts.contains("-next")) {
			jobParameters = new JobParametersBuilder(jobParameters, jobExplorer)
					.getNextJobParameters(job)
					.toJobParameters();
		}

		JobExecution jobExecution = launcher.run(job, jobParameters);
		return exitCodeMapper.intValue(jobExecution.getExitStatus().getExitCode());

	}
	catch (Throwable e) {
		String message = "Job Terminated in error: " + e.getMessage();
		logger.error(message, e);
		CommandLineJobRunner.message = message;
		return exitCodeMapper.intValue(ExitStatus.FAILED.getExitCode());
	}
	finally {
		if (context != null) {
			context.close();
		}
	}
}
```


CommandLineJobRunner原理就是通过Configuration得到ConfigurableApplicationContext，然后通过JobLauncher运行job


**运行：java -jar boot.jar jobConfiguration ,jobClassName ,jobParameters**
```
java -jar spring-batch-run-1.0.jar

14:55:04.085 [main] ERROR org.springframework.batch.core.launch.support.CommandLineJobRunner - At least 2 arguments are required: JobPath/JobClass and jobIdentifier.


//正确样例
java -jar spring-batch-run-1.0.jar com.czarea.springbatch.run.config.BatchConfiguration job1 message=czarea

```

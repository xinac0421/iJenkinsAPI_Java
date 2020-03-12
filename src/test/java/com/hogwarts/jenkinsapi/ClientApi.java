package com.hogwarts.jenkinsapi;

import com.hogwarts.tools.JSONParser;
import com.hogwarts.tools.Timer;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class ClientApi {
    private String url = "";
    public static Logger logger = Logger.getLogger(ClientApi.class);
    private String username;
    private String password;
    private String host;
    private String port;
    private String jobName;
    private static final String TYPE_TEXT = "plan/text";
    private static final String TYPE_JSON = "application/json";

    public String getJobName() {
        return jobName;
    }


    public ClientApi() {
        String propFileName = "ijenkins_config.properties";
        Properties prop = loadFromEnvProperties(propFileName);
        username = prop.getProperty("username");
        password = prop.getProperty("password");
        host = prop.getProperty("host");
        port = prop.getProperty("port");
        jobName = prop.getProperty("job_name");
    }

    public int getLastBuildNuber() throws Exception {
        String path = "job/" + jobName + "/lastBuild/buildNumber";
        return new Integer(get(path, TYPE_TEXT)).intValue();
    }

    public void runBuild() throws Exception {
        String path = "job/" + jobName + "/build";
        post(path, TYPE_TEXT);
    }

    public boolean isJobBuilding(int buildNumber) throws Exception {
        String path = "job/" + jobName + "/" + buildNumber + "/api/json";
        String rJson = get(path, TYPE_JSON);
        String isBuilding = JSONParser.getJsonValue(rJson, "building");
        return isBuilding == "true" ? true : false;
    }

    public String getJobResult(int buildNumber) throws Exception {
        String path = "job/" + jobName + "/" + buildNumber + "/api/json";
        String rJson = get(path, TYPE_JSON);
        String jobResult = JSONParser.getJsonValue(rJson, "result");
        return jobResult;
    }


    public String get(String path, String enctype) throws Exception {
        String url = "http://" + host + ":" + port + "/" + path;
        Client client = null;
        ClientResponse response = null;
        String rs = "";
        try {
            client = Client.create();
            client.addFilter(new HTTPBasicAuthFilter(username, password));
            WebResource webResource = client.resource(url);
            response = webResource.accept(enctype).get(ClientResponse.class);
            if (response != null && response.getStatus() >= 200 && response.getStatus() <= 206) {
                rs = response.getEntity(String.class);
            } else {
                throw new Exception("Response status error for running " + url + " task!");
            }
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.destroy();
            }
        }

        return rs;
    }

    public void post(String path, String enctype) throws Exception {
        String url = "http://" + host + ":" + port + "/" + path;
        Client client = null;
        ClientResponse response = null;
        try {
            client = Client.create();
            client.addFilter(new HTTPBasicAuthFilter(username, password));
            WebResource webResource = client.resource(url);
            response = webResource.accept(enctype).post(ClientResponse.class);
            if (response == null || response.getStatus() < 200 || response.getStatus() > 206) {
                throw new Exception("Response status error for running " + url + " task!");
            }
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.destroy();
            }
        }
    }

    private Properties loadFromEnvProperties(String propFileName) {
        Properties prop = null;

        String path = System.getProperty("user.home");

        //读入envProperties属性文件
        try {
            prop = new Properties();
            InputStream in = new BufferedInputStream(
                    new FileInputStream(path + File.separator + propFileName));
            prop.load(in);
            in.close();
        } catch (IOException ioex) {
            logger.error("配置文件加载失败，请检查 " + path + File.separator + propFileName + "文件是否存在！");
        }

        return prop;
    }


    public static void main(String[] args) throws Exception {
        ClientApi clientApi = new ClientApi();
        int maxWaitTime = 60; //60秒超时

        //获取当前lastBuildNumber
        int oldBuildNumber = clientApi.getLastBuildNuber();

        //启动任务
        clientApi.runBuild();
        logger.info("启动任务：" + clientApi.getJobName());

        //获取最新任务编号
        int newBuildNumber = clientApi.getLastBuildNuber();
        long start = System.currentTimeMillis();
        while(!(newBuildNumber > oldBuildNumber)){
            Timer.wait(2);
            newBuildNumber = clientApi.getLastBuildNuber();

            //判断超时
            long end = System.currentTimeMillis();
            if(end - start > maxWaitTime * 1000){
                throw new TimeoutException(maxWaitTime + "秒超时");
            }
        }
        logger.info("新任务编号：" + newBuildNumber);

        //等待任务执行完毕
        boolean buildingStatus = clientApi.isJobBuilding(newBuildNumber);
        start = System.currentTimeMillis();
        while (buildingStatus){
            Timer.wait(2);
            logger.info("任务" + clientApi.getJobName() + "正在运行 ...");
            buildingStatus = clientApi.isJobBuilding(newBuildNumber);

            //判断超时
            long end = System.currentTimeMillis();
            if(end - start > maxWaitTime * 1000){
                throw new TimeoutException(maxWaitTime + "秒超时");
            }
        }

        //任务运行完毕，获取任务结果
        String buildResult = clientApi.getJobResult(newBuildNumber);
        logger.info("任务" + clientApi.getJobName() + "运行完毕，最新任务编号：" + newBuildNumber + ", 运行结果：" + buildResult);

    }
}
package com.hogwarts.jenkins;

import com.hogwarts.common.IJenkinsAPI;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

public class ClientApi {
    private String url = "";
    private Logger logger = Logger.getLogger(IJenkinsAPI.class);
    private String username;
    private String password;
    private String host;
    private String port;
    private String jobName;
    private static final String EXPECTED_MIME_TYPE = "plan/text";

    public ClientApi(){
        String propFileName = "ijenkins_config.properties";
        Properties prop = loadFromEnvProperties(propFileName);
        username = prop.getProperty("username");
        password = prop.getProperty("password");
        host = prop.getProperty("host");
        port = prop.getProperty("port");
        jobName = prop.getProperty("job_name");
    }

    public String getLastBuildNuber() throws Exception{
        String path = "job/" + jobName + "/lastBuild/buildNumber";
        return get(path);
    }

    public void runBuild() throws Exception{
        String path = "job/" + jobName + "/build";
        post(path);
    }


    public String get(String path) throws Exception{
        String url = "http://" + host + ":" + port + "/" + path;
        Client client = null;
        ClientResponse response = null;
        String rs = "";
        try {
            client = Client.create();
            client.addFilter(new HTTPBasicAuthFilter(username, password));
            WebResource webResource = client.resource(url);
            response = webResource.accept(EXPECTED_MIME_TYPE).get(ClientResponse.class);
            if (response != null && response.getStatus() >= 200 && response.getStatus() <= 206) {
                rs = response.getEntity(String.class);
            }else{
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
    public void post(String path) throws Exception {
        String url = "http://" + host + ":" + port + "/" + path;
        Client client = null;
        ClientResponse response = null;
        try {
            client = Client.create();
            client.addFilter(new HTTPBasicAuthFilter(username, password));
            WebResource webResource = client.resource(url);
            response = webResource.accept(EXPECTED_MIME_TYPE).post(ClientResponse.class);
            if (response == null || response.getStatus() < 200 ||  response.getStatus() > 206) {
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




    public static void main(String[] args) throws Exception{
        ClientApi clientApi = new ClientApi();
        String curBuildNumber = clientApi.getLastBuildNuber();
    }
}
/**
 * JobRequirements.java
 * 
 * Iteratively users REST APIs to get all build plans in Bamboo, then each job contained.  Using a list of job keys,
 * a final step uses a private REST API to set a new requirement.
 * 
 * @author michael.howard
 * 
 */
package requests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * JobRequirements class definition.  A .credentials file MUST be present in the root of the project containing username=XXX and password=YYY.
 * Also, the maxResults defined as a static class member represents the page size of the REST GET for plans.  Please make sure this value is
 * always greater than the number of plans configured in Bamboo.  Since no page handling is being done on the GET response, if the maxResults
 * is too low, you will miss the trunctated plans.
 *
 */
public class JobRequirements {

    public static String baseUrl = "https://bamboo.trustvesta.com/bamboo";
//    public static String baseUrl = "http://tddvbamboo-a.ad.trustvesta.com:8085/bamboo";  // Staging Bamboo

    public static String user = "michael.howard";
    public static String maxResults = "999";
    
    /**
     * Run main to execute REST requests
     */
    public static void main(final String[] args) throws Exception {
        
        // Fetch exiting Bamboo plans
        JsonObject credentials = new JobRequirements().getCredentials();
        JsonObject plans = new JobRequirements().getPlans(credentials);
        ArrayList<String> planList = new JobRequirements().parsePlans(plans);
        System.out.println("size: " + planList.size() + "\n" + planList);
        ArrayList<String> jobs = new JobRequirements().getJobs(planList, credentials);
        ArrayList<String> jobList = new JobRequirements().parseJobs(jobs);
        System.out.println("size: " + jobList.size() + "\n" + jobList);
        new JobRequirements().setRequirement(jobList, credentials);

    }

    /**
     * getCredentials()
     * 
     * Fetches username and password from .credentials which lives in the project root.
     * 
     * @return JSON object containing username and password.
     */
    JsonObject getCredentials() {
        //Credentials are read from the '.credentials' file.
        JsonObject credentials = new JsonObject();
        String filename = ".credentials";
        String line = null;
        
        try {
            FileReader fileReader = new FileReader(filename);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while((line = bufferedReader.readLine()) != null) {
                String parts[] = line.split("=");
                credentials.addProperty(parts[0], parts[1]);
            }
            bufferedReader.close();
        } catch (Exception e) {
            System.out.println("Exception in getCredentials(): " + e);
        }
        
        return credentials;
    }
    
    /**
     * getPlans()
     * 
     * Using the passed in credentials and the baseUrl, build and execute a RESTful GET to Bamboo which returns a JSON
     * object containing all configured plans.
     * 
     * @param credentials
     * @return JSON object representing all plans
     */
    JsonObject getPlans(JsonObject credentials) {
        String response = "";
        try {
            String userPassword = credentials.get("username").getAsString() + ":" + credentials.get("password").getAsString();
            String encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

            URL url = new URL(baseUrl + "/rest/api/latest/plan?max-results=" + maxResults);
            
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Basic " + encoding);
            
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response += responseLine;
            }
            conn.disconnect();
            
        } catch (Exception e) {
            System.out.println("Exception in getPlans(): " + e);
        }
        
        return (JsonObject)new JsonParser().parse(response);
    }
    
    /**
     * parsePlans()
     * 
     * Read in a JSON object containing all build plans in Bamboo and then parse.  Certain build plans may 
     * be filtered (ie CI project here).  It returns an ArrayList with only the plan key string in each
     * element.
     *  
     * @param plans
     * @return ArrayList represending a since plan key per array element
     */
    ArrayList<String> parsePlans(JsonObject plans) {
        int startIndex = plans.get("plans").getAsJsonObject().get("start-index").getAsInt();
        int maxResult = plans.get("plans").getAsJsonObject().get("max-result").getAsInt();
        JsonArray planArray = plans.get("plans").getAsJsonObject().getAsJsonArray("plan");
       
        ArrayList<String> keyList = new ArrayList<String>();
        for (int i=startIndex; i<maxResult; i++) {
            String key = planArray.get(i).getAsJsonObject().get("key").getAsString();
            if ( !key.startsWith("CI-") ) {  // exclude these plans (CI project)
                keyList.add(key);
            }
        }
        
        return keyList;
    }
    
    /**
     * getJobs()
     * 
     * Given the ArrayList of plan keys and the user credentials, this method iterates over each plan key and performs a 
     * RESTful GET to pull all jobs configured for that plan.  It returns an ArrayList with each element containing a JSON
     * description of a single job.
     * 
     * @param planList
     * @param credentials
     * 
     * @return ArrayList representing a JSON object for each job.
     */
    ArrayList<String> getJobs(ArrayList<String> planList, JsonObject credentials ) {
        ArrayList<String> jobDescList = new ArrayList<String>();
        
        for (int i=0; i<planList.size(); i++) {
            String key = planList.get(i);
            String response = "";
            
            try {
                String userPassword = credentials.get("username").getAsString() + ":" + credentials.get("password").getAsString();
                String encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

                URL url = new URL(baseUrl + "/rest/api/latest/search/jobs/" + key);
                
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Basic " + encoding);
                
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream())));
                
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response += responseLine + "\n";
                }
                conn.disconnect();
                jobDescList.add(response);
                
            } catch (Exception e) {
                System.out.println("Exception in getJobs(): " + e);
            }
        }
        
        return jobDescList;
    }
    
    /**
     * parseJobs()
     * 
     * Given an ArrayList of JSON job descriptions, this method parses out each one.  It builds another ArrayList 
     * that only contains the job key.  Job name is also read and filtered out if it doesn't contain "Default" or
     * "Production"
     * 
     * @param jobs
     * @return
     */
    ArrayList<String> parseJobs( ArrayList<String> jobs) {
        ArrayList<String> jobList = new ArrayList<String>();
        
        for (int i=0; i<jobs.size(); i++) {
            
            try {
                JsonObject planJobs = (JsonObject)new JsonParser().parse(jobs.get(i));
                int size = planJobs.get("size").getAsInt();
                JsonArray jobsArray = planJobs.getAsJsonArray("searchResults");
                for (int j=0; j<size; j++) {
                    JsonObject job = jobsArray.get(j).getAsJsonObject();
                    String jobName = job.get("searchEntity").getAsJsonObject().get("jobName").getAsString();
                    if (jobName.toLowerCase().contains("default") || jobName.toLowerCase().contains("production")) {
                        jobList.add(job.get("searchEntity").getAsJsonObject().get("key").getAsString());
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception in parseJobs(): " + e);
            }
        }
        
        return jobList;
    }
    
    /**
     * setRequirements()
     * 
     * Given an ArrayList of job keys and the credentials, this method invokes a private Bamboo API to add a 
     * new requirement to each job.  It performs a RESTful POST using each job key and adds the "package_release"
     * requirement.
     * 
     * Note that if the requirement is already there, a 400 response is sent back and the POST operation throws
     * an exception.  This is not being directly handled but it's ok.  Since it's looping over all the job keys,
     * if 1 fails and throws an exception, it's caught and just moves to the next job key.  Keep an eye on the
     * console for what's happening here.
     * 
     * @param jobList
     * @param credentials
     */
    void setRequirement(ArrayList<String> jobList, JsonObject credentials) {
        
        for (int i=0; i<jobList.size(); i++) {
            String jobName = jobList.get(i);
            String response = "";
            
            try {
                String userPassword = credentials.get("username").getAsString() + ":" + credentials.get("password").getAsString();
                String encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

                URL url = new URL(baseUrl + "/rest/api/latest/config/job/" + jobName + "/requirement");
                
                JsonObject postData = new JsonObject();
                postData.addProperty("key" , "package_release");
                postData.addProperty("matchType", "EXISTS");
                
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Basic " + encoding);
                

                conn.setDoOutput( true );
                conn.setInstanceFollowRedirects( false );
                conn.setRequestProperty( "Content-Type", "application/json"); 
                conn.setRequestProperty( "charset", "utf-8");
                conn.setRequestProperty( "Content-Length", String.valueOf( postData.size() ));
                conn.setUseCaches( false );
                conn.getOutputStream().write(postData.toString().getBytes("UTF-8"));
           
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream())));
                
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response += responseLine + "\n";
                }
                conn.disconnect();
                System.out.println("Response after setting requirement: " + response);
                
            } catch (Exception e) {
                System.out.println("Exception in setRequirement(): " + e);
            }
        }
        return;
    }

}

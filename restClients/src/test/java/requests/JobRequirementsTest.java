package requests;

import requests.JobRequirements;

import java.util.ArrayList;

import org.junit.Test;

import com.atlassian.bamboo.specs.api.exceptions.PropertiesValidationException;
import com.google.gson.JsonObject;

public class JobRequirementsTest {
    @Test
    public void checkYourPlanOffline() throws PropertiesValidationException {
        // Fetch exiting Bamboo plans
        JsonObject credentials = new JobRequirements().getCredentials();
        JsonObject plans = new JobRequirements().getPlans(credentials);
        ArrayList<String> planList = new JobRequirements().parsePlans(plans);
        System.out.println("size: " + planList.size() + "\n" + planList);
        ArrayList<String> jobs = new JobRequirements().getJobs(planList, credentials);
        ArrayList<String> jobList = new JobRequirements().parseJobs(jobs);
        System.out.println("size: " + jobList.size() + "\n" + jobList);

    }
}

package tech.testra.jvm.apiv2.client.api;


import static tech.testra.jvm.apiv2.util.PropertyHelper.prop;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.testra.jvm.client.ApiException;
import tech.testra.jvm.client.api.ProjectApi;
import tech.testra.jvm.client.model.Project;
import tech.testra.jvm.client.model.ProjectRequest;

public final class TestraRestClientV2 {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestraRestClientV2.class);
  private static final String TESTRA_HOST= prop("host");
  private static final String PROJECTID = "projectID";
  private static final String EXECUTIONID = "executionID";
  private static String projectIDString;
  private ProjectApi projectApi = new ProjectApi();


  public TestraRestClientV2(){
    projectApi.getApiClient().setDebugging(true);
  }



  public String getProjectID(String projectName){
    List<Project> projects = new ArrayList<>();
    try {
      projects = projectApi.projectsGet();
    } catch (ApiException e) {
      e.printStackTrace();
    }
    if(projects.stream().anyMatch(x -> x.getName().equals(projectName))){
      return projects.stream().filter(x -> x.getName().equals(projectName)).collect(
          Collectors.toList()).get(0).getId();
    }
    else{
      LOGGER.error("No project found with name " + projectName);
      throw new IllegalArgumentException("Unknown project");
    }
  }

}

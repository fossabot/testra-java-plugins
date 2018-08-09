package tech.testra.jvm.plugin.cucumberv3.utils;

import cucumber.api.Result;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import tech.testra.java.client.model.DataTableRow;

@Data
public class StepTemplate {
  private int index;
  private String gherkinStep;
  private int line;
  private Result.Type status;
  private long stepDuration;
  private List<DataTableRow> dataTableRowList = new ArrayList<>();
}

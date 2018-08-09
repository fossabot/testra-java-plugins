@exampleUnit
Feature: Example Test Feature

  Background:
    And dataTable is readable hello
      | hello | world  |
    Given 3 plus 3 equals 6
    Given 4 plus 4 equals 8

  Scenario: First example scenario
    Then 4 minus 2 equal 2
    And fake step

  Scenario: Second example scenario
    Then 6 minus 4 equals 2

  @tag1
  Scenario: Third example scenario
    And 4 minus 2 equals 1
    And 4 minus 0 equals 4
    Then 2 plus 2 does not equal 5

  Scenario: Fourth example scenario
    And 4 minus 1 equals 3
    Then 2 plus 2 does not equal 5

  Scenario Outline: Outline example scenario - <col3>
    And 4 minus 1 equals 3
    Then <col1> plus <col2> equals <col3>
    Examples:
    |col1|col2|col3|
    |1   |1   |2   |
    |1   |2   |3   |

  @ExpectedFailure
  Scenario: Arguement types
    And true is true
    Then 4 minus 2 equals 2

  @Manual
  Scenario: Manual Test
    And true is true
    Then 4 minus 2 equals 2

  Scenario: DataTables
    And dataTable is readable hello
    | hello | world  |
    | hello | cheese |


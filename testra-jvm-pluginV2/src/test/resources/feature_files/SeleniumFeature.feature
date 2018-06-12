@selenium
Feature: Example Test Feature

  Background:
    Given the webdriver is loaded

  Scenario: click not existing element
    And the webpage is open
    Then the user can click the shopping button
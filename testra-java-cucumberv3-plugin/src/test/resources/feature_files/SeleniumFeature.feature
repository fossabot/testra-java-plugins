@selenium
Feature: Example Selenium Feature

  Background:
    Given the webdriver is loaded

  Scenario: Open site
    Then the webpage is open

  Scenario: click not existing element
    And the webpage is open
    Then the user can click the shopping button
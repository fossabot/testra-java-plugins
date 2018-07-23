Meta:
@Example

Scenario: Add elements to empty stack
Given an empty stack
When I add 2 elements
Then the stack should have 2 elements

Scenario: Clear stack
Given a stack with elements
When I clear stack
Then the stack should have 0 elements

Scenario: Failed Test
Given an empty stack
When I add 5 elements
Then the stack should have 1 elements

Scenario: Manual skippable
Meta:
@Manual
Given an empty stack
Then the stack should have 2 elements

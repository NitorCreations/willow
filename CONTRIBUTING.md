# How to contribute to Willow #

 1. Fork the repository from Github
 1. Do your proposed feature / fix
    * You will want to check from waffle if something like your feature is
      being worked on or proposed and discuss it there
 1. Create a Pull Request from your work
    * Our CI will pick the PR up and execute a build with tests and static
      code analysis
    * All of the tests need to pass and no new static code analysis issues may
      be reported. If there is a false positive, please commit the suppression
      of that including the justification. FindBugs and PMD issues are supressed
      with annotations in the relevant code, and coverity issues are supressed
      in coverity.yml in the root of the project. All suppressions will be
      reviewed before being accepted.
 1. Add the label 'ready to merge' to your pull request once you are confident
    that your fork is ready to be merged taking into account all of the above.
 1. Your PR will be picked up and commented if necessary or merged as is

# Links to the tools #

[![Stories in Ready](https://badge.waffle.io/NitorCreations/willow.png?label=ready&title=Ready)](https://waffle.io/NitorCreations/willow)
[![Code Advisor On Demand Status](https://badges.ondemand.coverity.com/streams/jdq5h6193p18d9k86859ro7t0c)](https://ondemand.coverity.com/streams/jdq5h6193p18d9k86859ro7t0c/jobs)
[ ![Codeship Status for NitorCreations/willow](https://codeship.com/projects/eafd7080-e03e-0132-ef42-7a41f362b68c/status?branch=master)](https://codeship.com/projects/80769)

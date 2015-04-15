module.exports = function (grunt) {
  grunt.initConfig({
    svgstore: {
      options: {
        prefix : 'shape-', // This will prefix each <g> ID
      },
      default : {
        files: {
          'icons.svg': ['originals/*.svg'],
        }
      }
    }
  });
  grunt.loadNpmTasks('grunt-svgstore');
  grunt.registerTask('default', ['svgstore']);
};

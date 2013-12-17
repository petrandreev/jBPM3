#
# $Id: $ 
#

1. Edit your subversion config ~/.subversion/config and set the following in each section:

[miscellany]
enable-auto-props = yes

[auto-props]
*.bat = svn:keywords=Id Revision;svn:eol-style=LF
*.java = svn:keywords=Id Revision;svn:eol-style=LF
*.sh = svn:keywords=Id Revision;svn:eol-style=LF
*.txt = svn:keywords=Id Revision;svn:eol-style=LF
*.wsdl = svn:keywords=Id Revision;svn:eol-style=LF
*.xml = svn:keywords=Id Revision;svn:eol-style=LF
*.xsd = svn:keywords=Id Revision;svn:eol-style=LF

2. If you are using an IDE, make sure that it refers to the same config. So, 
for example, if you are using Eclipse with Subclipse on windows, you need to
set Team->SVN->Configuration Location to:

/home/<your user name>/.subversion

3. If you are using Eclipse, set your code style to jbpm.code.templates.xml with 
Window --> Preferences --> Java --> Code Style --> Formatter --> Import 

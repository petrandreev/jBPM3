1. Edit your subversion configuration ~/.subversion/config
Set the following properties in their corresponding section:

[miscellany]
enable-auto-props = yes

[auto-props]
*.bat  = svn:eol-style=CRLF
*.java = svn:eol-style=native
*.properties = svn:eol-style=native
*.sh   = svn:eol-style=LF;svn:executable
*.txt  = svn:eol-style=native
*.xml  = svn:eol-style=native
*.xsd  = svn:eol-style=native

2. If you are using an IDE, make sure that it refers to the same config.
With Eclipse and Subclipse this is already the case if 
Preferences -> Team -> SVN -> Configuration location is set to 
"Use default config location"

3. If you are using Eclipse, import jbpm.code.templates.xml with
Preferences -> Java -> Code Style -> Code Templates -> Import
and jbpm.code.style.xml with 
Preferences -> Java -> Code Style -> Formatter -> Import 

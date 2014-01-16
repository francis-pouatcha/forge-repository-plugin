package org.adorsys.forge.plugins.tst;

import java.io.FileNotFoundException;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.ResourceFilter;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;

@Alias("repotest")
@RequiresProject
@RequiresFacet({ JavaSourceFacet.class, RepoTestFacet.class })
@Help("This plugin will help you setting up Arquillian tests.")
public class RepoTestPlugin implements Plugin {

	@Inject
	private Project project;

	@Inject
	private Shell shell;

	@Inject
	private JUnitTestGenerator junitTestGenerator;

	@Inject
	private Event<InstallFacets> request;

	@Inject
	private Event<JavaResource> generatedEvent;

	@Inject
	JavaSourceFacet java;

	@Inject
	@Current
	private Resource<?> currentResource;

	@SetupCommand
	public void installContainer() throws Exception {
		if(!project.hasFacet(RepoTestFacet.class)){
			shell.execute("arquillian setup --containerType REMOTE --containerName JBOSS_AS_REMOTE_7.X");

			request.fire(new InstallFacets(RepoTestFacet.class));
		}
	}

	@Command(value = "create-test", help = "Create a new test class with a default @Deployment method")
	public void createTest(
			@Option(name = "class", required = false, type = PromptType.JAVA_CLASS) JavaResource classToTest,
			@Option(name = "packages", required = false, type = PromptType.FILE_PATH) Resource<?> packageToTest,
			@Option(name="overrride", flagOnly=true, required=false ) boolean override,
			final PipeOut out) throws FileNotFoundException {

		if (classToTest != null)
			doCreateTest(out, classToTest, override);
		if (packageToTest != null) {
			if (!packageToTest.exists()) {
				ShellMessages.error(
						out,
						"Specified package "
								+ packageToTest.getFullyQualifiedName()
								+ " does not exist");
				return;
			}

			if (!(packageToTest instanceof DirectoryResource)) {
				ShellMessages.error(
						out,
						"Specified package "
								+ packageToTest.getFullyQualifiedName()
								+ " is not a directory");
				return;
			}

			processDirResource(out, (DirectoryResource) packageToTest, override);
		}

		if (classToTest == null && packageToTest == null) {
			if (currentResource != null) {
				if (currentResource instanceof JavaResource) {
					doCreateTest(out, (JavaResource) currentResource, override);
				} else if (currentResource instanceof DirectoryResource) {
					processDirResource(out, (DirectoryResource) currentResource, override);
				}
			}
		}
	}

	private void processDirResource(final PipeOut out,
			DirectoryResource resource, boolean override) {
		List<Resource<?>> javaResources = resource
				.listResources(new ResourceFilter() {
					@Override
					public boolean accept(final Resource<?> resource) {
						return resource instanceof JavaResource;
					}
				});
		if (javaResources != null) {
			for (Resource<?> resource2 : javaResources) {
				doCreateTest(out, (JavaResource) resource2, override);
			}
		}
		List<Resource<?>> dirResources = resource
				.listResources(new ResourceFilter() {
					@Override
					public boolean accept(final Resource<?> resource) {
						return resource instanceof DirectoryResource;
					}
				});
		for (Resource<?> resource2 : dirResources) {
			processDirResource(out, (DirectoryResource) resource2, override);
		}
	}

	private void doCreateTest(final PipeOut out, JavaResource classUnderTest, boolean override) {
		JavaSource<?> javaSource = null;
		try {
			javaSource = classUnderTest.getJavaSource();
		} catch (FileNotFoundException e) {
			ShellMessages.error(out, "source file for class not found. ["
					+ classUnderTest.getFullyQualifiedName() + "]");
		}

		JavaClass[] resources = junitTestGenerator.generateFrom(javaSource, out);
		if (resources == null || resources.length==0)
			return;

		final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

		for (JavaClass resource : resources) {
			JavaResource testJavaResource;
			try {
				testJavaResource = java.getTestJavaResource(resource);
			} catch (FileNotFoundException e1) {
				ShellMessages.error(out,
						"Can not read java source from generated class. ["
								+ resource.getQualifiedName() + "]");
				return;
			}
			try {
				if (!testJavaResource.exists()) {
					
					JavaResource savedTestJavaSource = java
							.saveTestJavaSource(resource);
					generatedEvent.fire(savedTestJavaSource);
					ShellMessages.success(out, "Generated class for ["
							+ javaSource.getQualifiedName() + "]");
				} else if (override){
					JavaResource savedTestJavaSource = java
							.saveTestJavaSource(resource);
					generatedEvent.fire(savedTestJavaSource);
					ShellMessages.success(out, "Generated class for ["
							+ javaSource.getQualifiedName() + "]. Overriding existing file.");
				} else {
					ShellMessages.info(out,
							"Generated class exists and will not be replaces. ["
									+ resource.getQualifiedName() + "]");
				}
			} catch (FileNotFoundException e) {
				ShellMessages.error(
						out,
						"Generated class is not in the src test path. ["
								+ resource.getQualifiedName() + "]");
				return;
			}
			
		}
	}
}

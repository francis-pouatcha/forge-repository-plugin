package org.adorsys.forge.plugins.repo;

import java.util.List;

import javax.inject.Inject;

import org.adorsys.forge.plugins.repo.RepositoryPlugin;
import org.adorsys.forge.plugins.repo.RepositoryGenerator;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.spec.javaee.PersistenceFacet;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.forge.test.SingletonAbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RepositoryPluginTest extends SingletonAbstractShellTest {

	@Inject
	private RepositoryGenerator repositoryGenerator;

	@Deployment
	public static JavaArchive getDeployment() {
		return AbstractShellTest.getDeployment().addPackages(true,
				RepositoryPlugin.class.getPackage());
	}

	@Before
	@Override
	public void beforeTest() throws Exception {
		super.beforeTest();
		initializeJavaProject();
		if ((getProject() != null)
				&& !getProject().hasFacet(PersistenceFacet.class)) {
			queueInputLines("", "", "");
			getShell()
					.execute(
							"persistence setup --provider HIBERNATE --container JBOSS_AS7");
		}
	}

	@Test
	public void testSetup() throws Exception {
		getShell().execute("set ACCEPT_DEFAULTS true");
		getShell().execute("repo setup");
	}

	@SuppressWarnings("unused")
	@Test
	public void testNewRepository() throws Exception {
		Project project = getProject();
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		JavaClass companyJPA = JavaParser.parse(JavaClass.class,
				RepositoryPluginTest.class
						.getResourceAsStream("CompanyJPA.java"));
		JavaInterface companyRepoJPA = repositoryGenerator.generateFrom(
				companyJPA, Long.class.getName());
		List<String> interfaces = companyRepoJPA.getInterfaces();
		Assert.assertNotNull(interfaces);
		Assert.assertEquals(1, interfaces.size());
	}

}

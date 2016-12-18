package com.github.blindpirate.gogradle

import com.github.blindpirate.gogradle.util.FileUtils
import com.github.blindpirate.gogradle.util.ReflectionUtils
import net.lingala.zip4j.core.ZipFile
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testfixtures.internal.ProjectBuilderImpl
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.InitializationError
import org.mockito.MockitoAnnotations

import java.nio.file.Path
import java.nio.file.Paths

import static com.github.blindpirate.gogradle.util.FileUtils.forceDelete

/**
 * <ul>
 *  <li>1.Check the usage of {@link WithProject} and create {@link Project} currentInstance if necessary.</li>
 *  <li>2.Check the usage of {@link WithResource} and copy (or unzip) resources to temp directory if necessary.</li>
 *  <li>3.Inject the project currentInstance and resource directory into the test class currentInstance.</li>
 *  <li>4.Inject fields annotated with{@link @Mock} and {@link @InjectMocks}.</li>
 *  <li>5.Clean up temp directories whenever necessary.</li>
 * <ul>
 */
// TODO: ignore @AccessWeb when offline
public class GogradleRunner extends BlockJUnit4ClassRunner {

    Object currentInstance

    // Every time we find a @WithProject, a new temp project folder,a new user home folder and
    // a new project currentInstance will be created
    // At the end of that method, these resources will be destroyed
    File projectDir
    File userhomeDir
    Project project

    // Every time we find a @WithResource, that resource will be copyed(or unzipped) to a temp dir
    // At the end of that method, these resource will be  destroyed
    File resourceDir

    public GogradleRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public Object createTest() throws Exception {
        currentInstance = super.createTest()
        MockitoAnnotations.initMocks(currentInstance)
        injectProjectAndResourceIfNecessary()
        return currentInstance;
    }

    def injectProjectAndResourceIfNecessary() {
        if (project != null) {
            ReflectionUtils.setField(currentInstance, 'project', project)
        }

        if (resourceDir != null) {
            ReflectionUtils.setField(currentInstance, 'resource', resourceDir)
        }
    }

    void cleanUpResource() {
        forceDelete(resourceDir)
        resourceDir = null
    }

    Object cleanUpProject() {
        forceDelete(projectDir)
        forceDelete(userhomeDir)
        projectDir = null
        userhomeDir = null
        project = null
    }

    File setUpResource(String resourceName) {
        File destDir = tmpRandomDirectory("resource");
        // when resource name is empty, the new created empty dir will be used
        if (resourceName.endsWith('zip')) {
            unzipResourceToDir(resourceName, destDir)
        } else if (resourceName != '') {
            copyResourceToDir(resourceName, destDir)
        }

        resourceDir = destDir
    }

    def copyResourceToDir(String resourceName, File destDir) {
        Path source = Paths.get(this.class.classLoader.getResource(resourceName).toURI())
        FileUtils.copyDirectory(source.toFile(), destDir)
    }

    def unzipResourceToDir(String resourceName, File destDir) {
        URI uri = this.class.classLoader.getResource(resourceName).toURI()
        ZipFile zipFile = new ZipFile(new File(uri))
        zipFile.extractAll(destDir.toString())
    }

    def setUpProject() {
        projectDir = tmpRandomDirectory('project')
        userhomeDir = tmpRandomDirectory('userhome')
        project = ProjectBuilder.builder()
                .withGradleUserHomeDir(userhomeDir)
                .withProjectDir(projectDir)
                .withName('test')
                .build()
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        // TODO not valid yet
        if (System.getProperty("TEST_ARE_OFFLINE")
                && findAnnoOnMethod(method, AccessWeb)) {
            notifier.fireTestIgnored(description)
            return
        }

        beforeOneTest(method)

        super.runChild(method, notifier);

        afterOneTest(method);
    }

    void afterOneTest(FrameworkMethod method) {
        WithResource withResource = findWithResource(method)
        if (withResource) {
            cleanUpResource()
        }
        WithProject withProject = findWithProject(method)
        if (withProject) {
            cleanUpProject()
        }
    }


    def beforeOneTest(FrameworkMethod method) {
        WithResource withResource = findWithResource(method);
        if (withResource) {
            setUpResource(withResource.value())
        }
        WithProject withProject = findWithProject(method)
        if (withProject) {
            setUpProject()
        }
    }

    WithProject findWithProject(FrameworkMethod method) {
        WithProject annoOnMethod = findAnnoOnMethod(method, WithProject)
        if (annoOnMethod) {
            return annoOnMethod
        }
        return findAnnoOnClass(method, WithProject)
    }

    WithResource findWithResource(FrameworkMethod method) {
        WithResource annoOnMethod = findAnnoOnMethod(method, WithResource)
        if (annoOnMethod) {
            return annoOnMethod
        }
        return findAnnoOnClass(method, WithResource)
    }

    def findAnnoOnMethod(FrameworkMethod method, Class clazz) {
        return method.method.getAnnotation(clazz)
    }

    def findAnnoOnClass(FrameworkMethod method, Class clazz) {
        return method.method.declaringClass.getAnnotation(clazz)
    }

    def tmpRandomDirectory(String prefix) {
        File ret = new File("build/tmp/${prefix}-${UUID.randomUUID()}")
        ret.mkdir()
        ret
    }
}

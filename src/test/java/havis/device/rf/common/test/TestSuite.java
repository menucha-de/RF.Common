package havis.device.rf.common.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/*
 * Call with VM args: -Djmockit-coverage-excludes=havis\.device\.rf\.daemon\.test\..* 
 * to exclude "test" subpackage from coverage report  
 */

@RunWith(Suite.class)
@SuiteClasses({ havis.device.rf.common.EnvironmentTest.class, havis.device.rf.common.BaudrateTest.class, havis.device.rf.common.CommunicationHandlerTest.class,
		havis.device.rf.common.ConfigurationManagerTest.class, havis.device.rf.common.KeepAliveThreadTest.class,
		havis.device.rf.common.MainControllerTest.class, havis.device.rf.common.util.FileUtilsTest.class, havis.device.rf.common.util.JsonSerializerTest.class,
		havis.device.rf.common.util.RFUtilsTest.class, havis.device.rf.common.tagsmooth.TagSmoothingHandlerTest.class })
public class TestSuite {

}

package com.thoughtworks.selenium.grid.hub.remotecontrol;

import com.thoughtworks.selenium.grid.hub.Environment;
import com.thoughtworks.selenium.grid.hub.EnvironmentManager;
import com.thoughtworks.selenium.grid.hub.NoSuchEnvironmentException;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import org.jbehave.classmock.UsingClassMock;
import org.jbehave.core.mock.Mock;
import org.junit.Test;

import java.util.List;


public class GlobalRemoteControlPoolTest extends UsingClassMock {

    @Test
    public void registerAddsTheRemoteControlToTheProvisioner() {
        final Mock provisioner;
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        provisioner = mock(RemoteControlProvisioner.class);
        pool = new GlobalRemoteControlPool() {
            public RemoteControlProvisioner getProvisioner(String environment) {
                assertEquals("an environment", environment);
                return (RemoteControlProvisioner) provisioner;
            }
        };
        provisioner.expects("add").with(remoteControl);

        pool.register(remoteControl);
        verifyMocks();
    }

    @Test
    public void unregisterReturnsTheResultOfProvisionerRemove() {
        final Mock provisioner;
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        provisioner = mock(RemoteControlProvisioner.class);
        pool = new GlobalRemoteControlPool() {
            public RemoteControlProvisioner getProvisioner(String environment) {
                assertEquals("an environment", environment);
                return (RemoteControlProvisioner) provisioner;
            }
        };

        provisioner.expects("remove").with(remoteControl).will(returnValue(false));
        assertEquals(false, pool.unregister(remoteControl));
        verifyMocks();
    }


    @Test
    public void unregisterRemovesTheRemoteControlToTheProvisioner() {
        final Mock provisioner;
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        provisioner = mock(RemoteControlProvisioner.class);
        pool = new GlobalRemoteControlPool() {
            public RemoteControlProvisioner getProvisioner(String environment) {
                assertEquals("an environment", environment);
                return (RemoteControlProvisioner) provisioner;
            }
        };

        provisioner.expects("remove").with(remoteControl).will(returnValue(true));
        assertTrue(pool.unregister(remoteControl));
        verifyMocks();
    }

    @Test
    public void onceUnregisteredARemoteControlIsNotassociatedWithAnyExistingSession() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new RemoteControlProxy("", 0, "", null);
        pool = new GlobalRemoteControlPool();

        pool.register(remoteControl);
        pool.associateWithSession(remoteControl, "a session id");
        pool.unregister(remoteControl);
        assertEquals(null, pool.retrieve("a session id"));
        verifyMocks();
    }

    @Test
    public void reserveReturnsTheRemoteControlReservedByTheProvisioner() {
        final Mock provisioner;
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        provisioner = mock(RemoteControlProvisioner.class);
        pool = new GlobalRemoteControlPool() {
            public RemoteControlProvisioner getProvisioner(String environment) {
                assertEquals("an environment", environment);
                return (RemoteControlProvisioner) provisioner;
            }
        };

        provisioner.expects("reserve").will(returnValue(remoteControl));
        assertEquals(remoteControl, pool.reserve(new Environment("an environment", "")));
        verifyMocks();
    }

    @Test
    public void reserveRaisesNoSuchEnvironmentExceptionWhenThereIsNoRegisteredRCForThisEnvironment() {
        final GlobalRemoteControlPool pool;

        pool = new GlobalRemoteControlPool() {
            public RemoteControlProvisioner getProvisioner(String environment) {
                assertEquals("an environment", environment);
                return null;
            }
        };

        try {
            pool.reserve(new Environment("an environment", ""));
            fail("did not catch NoSuchEnvironmentException as expected");
        } catch(NoSuchEnvironmentException e) {
            assertEquals("an environment", e.environment());  
        }
    }

    @Test
    public void getRemoteControlReturnsNullWhenNoRemoteControlHasBeenAssociatedWithTheSessionId() {
        assertEquals(null, new GlobalRemoteControlPool().retrieve("unknown session id"));
    }

    @Test
    public void getRemoteControlReturnsTheRemoteControlHasBeenAssociatedWithASpecificSession() {
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();
        final RemoteControlProxy remoteControl = new RemoteControlProxy("", 0, "", null);

        pool.associateWithSession(remoteControl, "a session id");
        assertEquals(remoteControl, pool.retrieve("a session id"));
    }

    @Test(expected = IllegalStateException.class)
    public void associateWithSessionThrowsAnIllegalStateExceptionWhenSessionIdIsAlreadyRegistered() {
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();

        pool.associateWithSession(new RemoteControlProxy("", 0, "", null), "shared session id");
        pool.associateWithSession(new RemoteControlProxy("", 0, "", null), "shared session id");
    }

    @Test
    public void associateWithSessionKeepsTrackOfEachSessionIndependently() {
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();
        final RemoteControlProxy firstRemoteControl = new RemoteControlProxy("", 0, "", null);
        final RemoteControlProxy secondRemoteControl = new RemoteControlProxy("", 0, "", null);

        pool.associateWithSession(firstRemoteControl, "first session id");
        pool.associateWithSession(secondRemoteControl, "second session id");

        assertEquals(firstRemoteControl, pool.retrieve("first session id"));
        assertEquals(secondRemoteControl, pool.retrieve("second session id"));
    }

    @Test
    public void afterReleaseForSessionARemoteControlIsNotAssociatedWithASessionAnymore() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;                                                

        remoteControl = new HealthyRemoteControl("", 0, "an environment", null);
        pool = new GlobalRemoteControlPool();
        pool.register(remoteControl);
        pool.reserve(new Environment("an environment", ""));
        pool.associateWithSession(remoteControl, "a session id");

        pool.releaseForSession("a session id");
        assertEquals(null, pool.retrieve("a session id"));
    }

    public void releaseForSessionDoesNotThrowsAnExceptionWhenSessionIsNotAssociated() {
        final GlobalRemoteControlPool pool = new GlobalRemoteControlPool();
        pool.releaseForSession("unknown session id");
    }

    @Test
    public void releaseForSessionReleasesTheRemoteControlOnTheProvisioner() {
        final Mock provisioner;
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        provisioner = mock(RemoteControlProvisioner.class);
        pool = new GlobalRemoteControlPool() {
            public RemoteControlProvisioner getProvisioner(String environment) {
                assertEquals("an environment", environment);
                return (RemoteControlProvisioner) provisioner;
            }
        };
        pool.register(remoteControl);
        pool.associateWithSession(remoteControl, "a session id");

        provisioner.expects("release").with(remoteControl);
        pool.releaseForSession("a session id");
        assertEquals(null, pool.retrieve("a session id"));
        verifyMocks();
    }

    @Test
    public void releaseReleasesTheRemoteControlOnTheProvisioner() {
        final Mock provisioner;
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new RemoteControlProxy("", 0, "an environment", null);
        provisioner = mock(RemoteControlProvisioner.class);
        pool = new GlobalRemoteControlPool() {
            public RemoteControlProvisioner getProvisioner(String environment) {
                assertEquals("an environment", environment);
                return (RemoteControlProvisioner) provisioner;
            }
        };

        provisioner.expects("release").with(remoteControl);

        pool.release(remoteControl);
        assertEquals(null, pool.retrieve("a session id"));
        verifyMocks();
    }

//    @Test
//    public void availableRemoteControlsReturnAvailableRemoteControlsOfTheProvisioner() {
//        final Mock provisioner;
//        final List<RemoteControlProxy> expectedList;
//        final GlobalRemoteControlPool pool;
//
//        expectedList = new ArrayList<RemoteControlProxy>();
//        provisioner = mock(RemoteControlProvisioner.class);
//        pool = new GlobalRemoteControlPool();
//        provisioner.expects("availableRemoteControls").will(returnValue(expectedList));
//
//        assertEquals(expectedList, pool.availableRemoteControls());
//        verifyMocks();
//    }

//    @Test
//    public void reservedRemoteControlsReturnAvailableRemoteControlsOfTheProvisioner() {
//        final Mock provisioner;
//        final List<RemoteControlProxy> expectedList;
//        final GlobalRemoteControlPool pool;
//
//        expectedList = new ArrayList<RemoteControlProxy>();
//        provisioner = mock(RemoteControlProvisioner.class);
//        pool = new GlobalRemoteControlPool();
//        provisioner.expects("reservedRemoteControls").will(returnValue(expectedList));
//
//        assertEquals(expectedList, pool.reservedRemoteControls());
//        verifyMocks();
//    }

    
    @Test
    public void reserveReturnsTheRemoteControlReservedOnEnvironmentPool() {
        final GlobalRemoteControlPool pool;
        final Environment environment;
        final RemoteControlProxy remoteControl;

        remoteControl = new HealthyRemoteControl("", 0, "an environment", null);
        environment = new Environment("an environment", "*firefox");
        pool = new GlobalRemoteControlPool();
        pool.register(remoteControl);

        assertEquals(remoteControl, pool.reserve(environment));
        verifyMocks();
    }

    @Test
    public void getPoolForSessionReturnsThePoolThatHaveBeenAssociatedWithASession() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new RemoteControlProxy("host", 0, "an environment", null);
        pool = new GlobalRemoteControlPool();
        pool.register(remoteControl);
        pool.associateWithSession(remoteControl, "a session id");

        assertSame(remoteControl, pool.getRemoteControlForSession("a session id"));
        verifyMocks();
    }

    @Test
    public void getPoolForSessionReturnsNullWhenNoPoolHaveBeenAssociatedWithThisSession() {
        assertNull(new GlobalRemoteControlPool().getRemoteControlForSession("a session id"));
    }

    @Test
    public void aRemoteControlIsNotAsssociateWithTheSesssionAfterCallingReleaseForSession() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new HealthyRemoteControl("host", 0, "an environment", null);
        pool = new GlobalRemoteControlPool();

        pool.register(remoteControl);
        pool.reserve(new Environment("an environment", ""));
        pool.associateWithSession(remoteControl, "a session id");
        pool.releaseForSession("a session id");

        assertEquals(null, pool.getRemoteControlForSession("a session id"));
    }

    @Test
    public void releaseDelegatesCallToEnvironmentPoolAndForgetsAboutThisSession() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;

        remoteControl = new HealthyRemoteControl("host", 0, "an environment", null);
        pool = new GlobalRemoteControlPool();

        pool.register(remoteControl);
        pool.reserve(new Environment("an environment", ""));
        pool.release(remoteControl);
        assertFalse(remoteControl.sesssionInProgress());
    }

    @Test
    public void getReturnsRemoteControlThatHaveBeenAssociatedWithASession() {
        final RemoteControlProxy remoteControl;
        final GlobalRemoteControlPool pool;
        remoteControl = new RemoteControlProxy("host", 0, "an environment", null);
        pool = new GlobalRemoteControlPool();

        pool.register(remoteControl);
        pool.associateWithSession(remoteControl, "a session id");

        assertEquals(remoteControl, pool.retrieve("a session id"));
    }

    @Test
    public void availableRemoteControlsReturnAnEmptyArrayWhenThereIsNoEnvironment() {
        assertTrue(new GlobalRemoteControlPool().availableRemoteControls().isEmpty());
    }

    @Test
    public void availableRemoteControlsReturnAvailableRemoteControlsForAllEnvironments() {
        final HealthyRemoteControl anotherRCForTheSecondEnvironment;
        final HealthyRemoteControl aRCForAnotherEnvironment;
        final List<RemoteControlProxy> availableRemoteControls;
        final EnvironmentManager environmentManager;
        final Environment firstEnvironment;
        final Environment secondEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl aRC;

        
        environmentManager = new EnvironmentManager();
        firstEnvironment = new Environment("first environment", "*chrome");
        secondEnvironment = new Environment("second environment", "*chrome");
        environmentManager.addEnvironment(firstEnvironment);
        environmentManager.addEnvironment(secondEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "first environment", null);
        aRCForAnotherEnvironment = new HealthyRemoteControl("second host", 0, "second environment", null);
        pool.register(aRC);
        pool.register(aRCForAnotherEnvironment);
        anotherRCForTheSecondEnvironment = new HealthyRemoteControl("third host", 0, "second environment", null);
        pool.register(anotherRCForTheSecondEnvironment);

        availableRemoteControls = pool.availableRemoteControls();
        assertEquals(3, availableRemoteControls.size());
        assertTrue(availableRemoteControls.contains(aRC));
        assertTrue(availableRemoteControls.contains(aRCForAnotherEnvironment));
        assertTrue(availableRemoteControls.contains(anotherRCForTheSecondEnvironment));
    }

    @Test
    public void availableRemoteControlsDoNotReturnReservedRemoteControls() {
        final List<RemoteControlProxy> availableRemoteControls;
        final EnvironmentManager environmentManager;
        final Environment anEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl anotherRC;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        anEnvironment = new Environment("an environment", "*chrome");
        environmentManager.addEnvironment(anEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "an environment", null);
        anotherRC = new HealthyRemoteControl("second host", 0, "an environment", null);
        pool.register(aRC);
        pool.register(anotherRC);
        pool.reserve(anEnvironment);

        availableRemoteControls = pool.availableRemoteControls();
        assertEquals(1, availableRemoteControls.size());
        assertFalse(availableRemoteControls.contains(aRC));
        assertTrue(availableRemoteControls.contains(anotherRC));
    }

    @Test
    public void availableRemoteControlsIsEmptyWhenAllRemoteControlsAreReserved() {
        final EnvironmentManager environmentManager;
        final Environment anEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl anotherRC;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        anEnvironment = new Environment("an environment", "*chrome");
        environmentManager.addEnvironment(anEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "an environment", null);
        anotherRC = new HealthyRemoteControl("second host", 0, "an environment", null);
        pool.register(aRC);
        pool.register(anotherRC);
        pool.reserve(anEnvironment);
        pool.reserve(anEnvironment);

        assertTrue(pool.availableRemoteControls().isEmpty());
    }

    @Test
    public void reservedRemoteControlsReturnAnEmptyArrayWhenThereIsNoEnvironment() {
        assertTrue(new GlobalRemoteControlPool().reservedRemoteControls().isEmpty());
        verifyMocks();
    }

    @Test
    public void reservedRemoteControlsReturnsReservedRemoteControlsForAllEnvironemnt() {
        final HealthyRemoteControl anotherRCForTheSecondEnvironment;
        final HealthyRemoteControl aRCForAnotherEnvironment;
        final List<RemoteControlProxy> reservedRemoteControls;
        final EnvironmentManager environmentManager;
        final Environment firstEnvironment;
        final Environment secondEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        firstEnvironment = new Environment("first environment", "*chrome");
        secondEnvironment = new Environment("second environment", "*chrome");
        environmentManager.addEnvironment(firstEnvironment);
        environmentManager.addEnvironment(secondEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "first environment", null);
        aRCForAnotherEnvironment = new HealthyRemoteControl("second host", 0, "second environment", null);
        pool.register(aRC);
        pool.register(aRCForAnotherEnvironment);
        anotherRCForTheSecondEnvironment = new HealthyRemoteControl("third host", 0, "second environment", null);
        pool.register(anotherRCForTheSecondEnvironment);

        pool.reserve(firstEnvironment);
        pool.reserve(secondEnvironment);
        pool.reserve(secondEnvironment);

        reservedRemoteControls = pool.reservedRemoteControls();
        assertEquals(3, reservedRemoteControls.size());
        assertTrue(reservedRemoteControls.contains(aRC));
        assertTrue(reservedRemoteControls.contains(aRCForAnotherEnvironment));
        assertTrue(reservedRemoteControls.contains(anotherRCForTheSecondEnvironment));
        verifyMocks();
    }

    @Test
    public void reservedRemoteControlsDoNotReturnAvailableRemoteControls() {
        final List<RemoteControlProxy> reservedRemoteControls;
        final EnvironmentManager environmentManager;
        final Environment anEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl anotherRC;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        anEnvironment = new Environment("an environment", "*chrome");
        environmentManager.addEnvironment(anEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "an environment", null);
        anotherRC = new HealthyRemoteControl("second host", 0, "an environment", null);
        pool.register(aRC);
        pool.register(anotherRC);
        pool.reserve(anEnvironment);

        reservedRemoteControls = pool.reservedRemoteControls();
        assertEquals(1, reservedRemoteControls.size());
        assertTrue(reservedRemoteControls.contains(aRC));
        assertFalse(reservedRemoteControls.contains(anotherRC));
        verifyMocks();
    }

    @Test
    public void reservedRemoteControlsIsEmptyWhenNoRemoteControlsAreReserved() {
        final EnvironmentManager environmentManager;
        final Environment anEnvironment;
        final GlobalRemoteControlPool pool;
        final HealthyRemoteControl anotherRC;
        final HealthyRemoteControl aRC;


        environmentManager = new EnvironmentManager();
        anEnvironment = new Environment("an environment", "*chrome");
        environmentManager.addEnvironment(anEnvironment);

        pool = new GlobalRemoteControlPool();
        aRC = new HealthyRemoteControl("first host", 0, "an environment", null);
        anotherRC = new HealthyRemoteControl("second host", 0, "an environment", null);
        pool.register(aRC);
        pool.register(anotherRC);

        assertTrue(pool.reservedRemoteControls().isEmpty());
    }

    @Test
    public void logSessionMapDoesNotBombWhenThereIsNoSession() {
        final GlobalRemoteControlPool pool;

        pool = new GlobalRemoteControlPool();
        pool.logSessionMap();
    }

    @Test
    public void logSessionMapDoesNotBombWhenThereIsSomeRegisteredSession() {
        final RemoteControlProxy aRemoteControl;
        final RemoteControlProxy anotherRemoteControl;
        final GlobalRemoteControlPool pool;

        aRemoteControl = new HealthyRemoteControl("host", 4444, "an environment", null);
        anotherRemoteControl = new HealthyRemoteControl("host", 4445, "an environment", null);
        pool = new GlobalRemoteControlPool();

        pool.register(aRemoteControl);
        pool.register(anotherRemoteControl);
        pool.reserve(new Environment("an environment", ""));
        pool.reserve(new Environment("an environment", ""));
        pool.associateWithSession(aRemoteControl, "a session id");
        pool.associateWithSession(anotherRemoteControl, "another session id");
        pool.logSessionMap();
    }

}
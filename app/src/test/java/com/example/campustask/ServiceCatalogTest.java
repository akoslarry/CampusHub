package com.example.campustask;

import com.example.campustask.model.CampusService;
import com.example.campustask.model.ServiceCatalog;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServiceCatalogTest {
    @Test
    public void defaultCatalogContainsCoreCampusServices() {
        List<CampusService> services = ServiceCatalog.defaultServices();

        assertTrue(ServiceCatalog.containsId(services, "schedule"));
        assertTrue(ServiceCatalog.containsId(services, "food"));
        assertTrue(ServiceCatalog.containsId(services, "classroom"));
        assertTrue(ServiceCatalog.containsId(services, "tasks"));
    }

    @Test
    public void searchMatchesNameAndDescription() {
        List<CampusService> result = ServiceCatalog.search(ServiceCatalog.defaultServices(), "预约");

        assertEquals(1, result.size());
        assertEquals("classroom", result.get(0).id);
    }

    @Test
    public void pinnedServicesComeBeforeRegularServices() {
        List<CampusService> services = ServiceCatalog.defaultServices();

        assertTrue(services.get(0).pinned);
        assertTrue(services.get(1).pinned);
    }
}

package com.example.campustask.model;

import java.util.ArrayList;
import java.util.List;

public final class ServiceCatalog {
    private ServiceCatalog() {
    }

    public static List<CampusService> defaultServices() {
        List<CampusService> services = new ArrayList<>();
        services.add(new CampusService("schedule", "课程表", "查看本周课程、节次、教室和周次", "学习", "课", 0xFF3B82F6, true));
        services.add(new CampusService("tasks", "待办提醒", "记录作业、考试和校园事务提醒", "学习", "办", 0xFF8B5CF6, false));
        services.add(new CampusService("classroom", "教室预约", "查看空闲教室并提交自习预约", "学习", "室", 0xFF10B981, false));
        return services;
    }

    public static boolean containsId(List<CampusService> services, String id) {
        for (CampusService service : services) {
            if (service.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static List<CampusService> search(List<CampusService> services, String query) {
        String keyword = query == null ? "" : query.trim();
        List<CampusService> result = new ArrayList<>();
        for (CampusService service : services) {
            if (keyword.isEmpty()
                    || service.name.contains(keyword)
                    || service.description.contains(keyword)
                    || service.category.contains(keyword)) {
                result.add(service);
            }
        }
        return result;
    }
}

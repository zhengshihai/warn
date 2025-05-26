package com.tianhai.warn.enums;

import lombok.Getter;

/**
 * 学院枚举类
 */
@Getter
public enum CollegeEnum {
    COMPUTER_SCIENCE("CS", "计算机学院"),
    MECHANICAL_ENGINEERING("ME", "机械工程学院"),
    ELECTRICAL_ENGINEERING("EE", "电气工程学院"),
    CIVIL_ENGINEERING("CE", "土木工程学院"),
    CHEMICAL_ENGINEERING("CHE", "化学工程学院"),
    ECONOMICS("ECO", "经济学院"),
    BUSINESS("BUS", "商学院"),
    LAW("LAW", "法学院"),
    MEDICINE("MED", "医学院"),
    ARTS("ART", "艺术学院"),
    FOREIGN_LANGUAGES("FL", "外国语学院"),
    MATHEMATICS("MATH", "数学学院"),
    PHYSICS("PHY", "物理学院"),
    LIFE_SCIENCES("LS", "生命科学学院"),
    ENVIRONMENTAL_SCIENCE("ES", "环境学院"),
    INFORMATION_ENGINEERING("IE", "信息工程学院"),
    MATERIALS_SCIENCE("MS", "材料科学与工程学院"),
    AGRICULTURE("AG", "农学院"),
    ARCHITECTURE("ARCH", "建筑学院"),
    PHARMACY("PHARM", "药学院");

    private final String code;
    private final String name;

    CollegeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据学院名称获取学院代码
     * 
     * @param name 学院名称
     * @return 学院代码，如果未找到返回null
     */
    public static String getCodeByName(String name) {
        for (CollegeEnum college : values()) {
            if (college.getName().equals(name)) {
                return college.getCode();
            }
        }
        return null;
    }

    /**
     * 根据学院代码获取学院名称
     * 
     * @param code 学院代码
     * @return 学院名称，如果未找到返回null
     */
    public static String getNameByCode(String code) {
        for (CollegeEnum college : values()) {
            if (college.getCode().equals(code)) {
                return college.getName();
            }
        }
        return null;
    }
}
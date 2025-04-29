package com.example.crypt;

public class ContainerData {
    private int size;
    private String name;
    private String algorithm;
    private String password;
    private String fsType;

    // Геттеры и сеттеры
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFsType() { return fsType; }
    public void setFsType(String fsType) { this.fsType = fsType; }
}
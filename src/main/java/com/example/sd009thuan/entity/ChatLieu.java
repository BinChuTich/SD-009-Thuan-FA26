//package com.example.sd009thuan.entity;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.Table;
//import lombok.Getter;
//import lombok.Setter;
//
//@Getter
//@Setter
//@Entity
//@Table
//public class ChatLieu {
//
//    @Size(max = 255)
//    @NotNull
//    @Nationalized
//    @Column(name = "ten_chat_lieu", nullable = false)
//    private String tenChatLieu;
//
//    @NotNull
//    @ColumnDefault("1")
//    @Column(name = "trang_thai", nullable = false)
//    private Integer trangThai;
//
//    @OneToMany(mappedBy = "idChatLieu")
//    private Set<SanPham> sanPhams = new LinkedHashSet<>();
//
//
//}
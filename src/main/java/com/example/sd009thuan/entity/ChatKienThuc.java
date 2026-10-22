package com.example.sd009thuan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "chat_kien_thuc")
public class ChatKienThuc {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chu_de", nullable = false, length = 200)
    private String chuDe;

    @Column(name = "tu_khoa", nullable = false, length = 500)
    private String tuKhoa;

    @Column(name = "cau_tra_loi", nullable = false, columnDefinition = "nvarchar(max)")
    private String cauTraLoi;

    @Column(name = "kich_hoat", nullable = false)
    private Boolean kichHoat = true;
}

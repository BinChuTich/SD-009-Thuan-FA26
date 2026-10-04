package com.example.sd009thuan.service;

import com.example.sd009thuan.entity.*;
import com.example.sd009thuan.repository.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ThuocTinhService {

    private final ChatLieuRepository chatLieuRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final XuatXuRepository xuatXuRepository;
    private final DanhMucRepository danhMucRepository;
    private final CoAoRepository coAoRepository;
    private final TayAoRepository tayAoRepository;
    private final HoaTietRepository hoaTietRepository;
    private final MauSacRepository mauSacRepository;
    private final KichCoRepository kichCoRepository;

    public ThuocTinhService(
            ChatLieuRepository chatLieuRepository,
            ThuongHieuRepository thuongHieuRepository,
            XuatXuRepository xuatXuRepository,
            DanhMucRepository danhMucRepository,
            CoAoRepository coAoRepository,
            TayAoRepository tayAoRepository,
            HoaTietRepository hoaTietRepository,
            MauSacRepository mauSacRepository,
            KichCoRepository kichCoRepository
    ) {
        this.chatLieuRepository = chatLieuRepository;
        this.thuongHieuRepository = thuongHieuRepository;
        this.xuatXuRepository = xuatXuRepository;
        this.danhMucRepository = danhMucRepository;
        this.coAoRepository = coAoRepository;
        this.tayAoRepository = tayAoRepository;
        this.hoaTietRepository = hoaTietRepository;
        this.mauSacRepository = mauSacRepository;
        this.kichCoRepository = kichCoRepository;
    }

    public Map<String, Object> getAllThuocTinh() {
        Map<String, Object> map = new HashMap<>();
        map.put("chatLieu", chatLieuRepository.findByTrangThai(1));
        map.put("thuongHieu", thuongHieuRepository.findByTrangThai(1));
        map.put("xuatXu", xuatXuRepository.findByTrangThai(1));
        map.put("danhMuc", danhMucRepository.findByTrangThai(1));
        map.put("coAo", coAoRepository.findByTrangThai(1));
        map.put("tayAo", tayAoRepository.findByTrangThai(1));
        map.put("hoaTiet", hoaTietRepository.findByTrangThai(1));
        map.put("mauSac", mauSacRepository.findByTrangThai(1));
        map.put("kichCo", kichCoRepository.findByTrangThai(1));
        return map;
    }

    public List<ChatLieu> getChatLieu() { return chatLieuRepository.findByTrangThai(1); }
    public List<ThuongHieu> getThuongHieu() { return thuongHieuRepository.findByTrangThai(1); }
    public List<XuatXu> getXuatXu() { return xuatXuRepository.findByTrangThai(1); }
    public List<DanhMuc> getDanhMuc() { return danhMucRepository.findByTrangThai(1); }
    public List<CoAo> getCoAo() { return coAoRepository.findByTrangThai(1); }
    public List<TayAo> getTayAo() { return tayAoRepository.findByTrangThai(1); }
    public List<HoaTiet> getHoaTiet() { return hoaTietRepository.findByTrangThai(1); }
    public List<MauSac> getMauSac() { return mauSacRepository.findByTrangThai(1); }
    public List<KichCo> getKichCo() { return kichCoRepository.findByTrangThai(1); }
}

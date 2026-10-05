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
        map.put("chatLieu", chatLieuRepository.findAll());
        map.put("thuongHieu", thuongHieuRepository.findAll());
        map.put("xuatXu", xuatXuRepository.findAll());
        map.put("danhMuc", danhMucRepository.findAll());
        map.put("coAo", coAoRepository.findAll());
        map.put("tayAo", tayAoRepository.findAll());
        map.put("hoaTiet", hoaTietRepository.findAll());
        map.put("mauSac", mauSacRepository.findAll());
        map.put("kichCo", kichCoRepository.findAll());
        return map;
    }

    public List<ChatLieu> getChatLieu() { return chatLieuRepository.findAll(); }
    public List<ThuongHieu> getThuongHieu() { return thuongHieuRepository.findAll(); }
    public List<XuatXu> getXuatXu() { return xuatXuRepository.findAll(); }
    public List<DanhMuc> getDanhMuc() { return danhMucRepository.findAll(); }
    public List<CoAo> getCoAo() { return coAoRepository.findAll(); }
    public List<TayAo> getTayAo() { return tayAoRepository.findAll(); }
    public List<HoaTiet> getHoaTiet() { return hoaTietRepository.findAll(); }
    public List<MauSac> getMauSac() { return mauSacRepository.findAll(); }
    public List<KichCo> getKichCo() { return kichCoRepository.findAll(); }
}

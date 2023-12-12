package com.example.web_project.services.Impl;



import com.example.web_project.entities.*;
import com.example.web_project.repository.*;
import com.example.web_project.services.PrebuyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PrebuyServiceImpl implements PrebuyService {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private BillInfoRepository billinfoRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DrinkcostRepository drinkCostRepository;
    @Autowired
    private DrinktypeRepository drinktypeRepository;



    @Override
    @Transactional
    public void addToOrder(int idProduct, int count, int idSize, int idAddin, int idFoam, int idTopping, int idAccount) {

        // Kiểm tra xem có tồn tại Bill chưa được thanh toán (paid = false) hay không
        Optional<Bill> bill = billRepository.findUnpaidBillByAccountId(idAccount).stream().findFirst();

        Bill existingBill = bill.orElse(null);

        if (existingBill != null) {
            // Nếu có, sử dụng Bill hiện tại để tạo BillInfo mới
            createBillInfo(existingBill, idProduct, count, idSize,idAddin,idFoam,idTopping);
        } else {
            // Nếu không có, tạo mới một Bill và sử dụng nó để tạo BillInfo mới
            Bill newBill = new Bill();
            Account account = accountRepository.findById(idAccount).orElse(null);
            newBill.setIdAccount(account);
            newBill.setPaid(false); // Gán paid cho Bill mới

            // Lưu Bill mới để tạo ra idBill
            billRepository.save(newBill);
            // Tạo BillInfo mới sử dụng Bill mới
            createBillInfo(newBill, idProduct, count, idSize, idAddin, idFoam, idTopping);
        }
    }

    private void createBillInfo(Bill bill, int idProduct, int count, int idSize, int idAddin, int idFoam, int idTopping) {
        // Tạo một Billinfo mới
        Billinfo billInfo = new Billinfo();
        billInfo.setBillID(bill);
        // Truy xuất Product thông qua idProduct
        Product product = productRepository.findById(idProduct).orElse(null);
        billInfo.setProductID(product);

        // Đặt các thuộc tính khác
        billInfo.setCount(count);

        Integer idDrinkType = findIDDrinkTypeBySizeAndAddinAndFoamAndTopping(idSize,idAddin,idFoam,idTopping);
        Drinktype drinkcost = drinktypeRepository.findById(idDrinkType).orElse(null);
        billInfo.setDrinkTypeID(drinkcost);

        // Lưu Billinfo để tạo ra idBillInfo
        billinfoRepository.save(billInfo);
    }

    @Override
    public Integer findIDDrinkTypeBySizeAndAddinAndFoamAndTopping(int idSize, int idAddin, int idFoam, int idTopping) {
        return drinkCostRepository.findIDDrinkTypeByIdSizeAndIdAddinAndIdFoamAndIdTopping(idSize, idAddin, idFoam, idTopping);
    }

    @Override
    public Double calculateTotalCost(int idSize, int idFoam, int idAddin, int idTopping, int idProduct) {
        // Lấy giá trị addcost từ bảng DrinkCost
        double addCost = drinkCostRepository.findTotalCostByIdSizeAndIdFoamAndIdAddinAndIdTopping(idSize, idFoam, idAddin, idTopping);
        // Lấy giá trị basecost từ bảng Product
        Integer baseCost = 0;

        AtomicReference<Integer> cost = null;
        productRepository.findById(idProduct).ifPresent(p -> cost.set(p.getCost()));
        baseCost = cost.get();
//        double baseCost = productRepository.findCost(idProduct);

        // Tính toán totalCost
        return (double) baseCost + addCost;
    }
}
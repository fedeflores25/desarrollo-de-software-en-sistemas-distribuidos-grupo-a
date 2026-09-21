
package com.rentar.service;
import com.rentar.dto.HistorialResponse;
import java.util.List;

public interface IHistorialService {
    List<HistorialResponse> obtenerHistorial(Long idCliente);
    
    List<HistorialResponse> obtenerHistorial();

}
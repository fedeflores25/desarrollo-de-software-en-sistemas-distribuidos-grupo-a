package com.rentar.service;

import com.rentar.dto.ReservaFiltro;
import com.rentar.dto.ConsultaReservaResponse;
import java.util.List;

public interface IConsultaReservaService {
    List<ConsultaReservaResponse> consultarReservas(ReservaFiltro filtro);
}

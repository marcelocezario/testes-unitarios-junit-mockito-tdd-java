package br.ce.wcaquino.servicos;

import static br.ce.wcaquino.builders.FilmeBuilder.umFilme;
import static br.ce.wcaquino.builders.FilmeBuilder.umFilmeSemEstoque;
import static br.ce.wcaquino.builders.UsuarioBuilder.umUsuario;
import static br.ce.wcaquino.matchers.MatchersProprios.caiEm;
import static br.ce.wcaquino.matchers.MatchersProprios.caiNumaSegunda;
import static br.ce.wcaquino.matchers.MatchersProprios.ehHoje;
import static br.ce.wcaquino.matchers.MatchersProprios.ehHojeComDiferencaDias;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import br.ce.wcaquino.builders.LocacaoBuilder;
import br.ce.wcaquino.daos.LocacaoDAO;
import br.ce.wcaquino.entidades.Filme;
import br.ce.wcaquino.entidades.Locacao;
import br.ce.wcaquino.entidades.Usuario;
import br.ce.wcaquino.exceptions.FilmeSemEstoqueException;
import br.ce.wcaquino.exceptions.LocadoraException;
import br.ce.wcaquino.utils.DataUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ LocacaoService.class })
public class LocacaoServiceTest {

	@InjectMocks
	private LocacaoService service;

	@Mock
	private LocacaoDAO dao;
	@Mock
	private SPCService spc;
	@Mock
	private EmailService emailService;

	@Rule
	public ErrorCollector error = new ErrorCollector();

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		service = PowerMockito.spy(service);
	}

	@Test
	public void deveAlugarFilme() throws Exception {

		// cen�rio
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().comValor(5.0).agora());

		//PowerMockito.whenNew(Date.class).withNoArguments().thenReturn(DataUtils.obterData(15, 11, 2021));
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, 15);
		calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
		calendar.set(Calendar.YEAR, 2021);
		PowerMockito.mockStatic(Calendar.class);
		PowerMockito.when(Calendar.getInstance()).thenReturn(calendar);

		// a��o
		Locacao locacao = service.alugarFilme(usuario, filmes);

		// verifica��o
		error.checkThat(locacao.getValor(), is(equalTo(5.0)));
		//error.checkThat(locacao.getDataLocacao(), ehHoje());
		//error.checkThat(locacao.getDataRetorno(), ehHojeComDiferencaDias(1));
		error.checkThat(DataUtils.isMesmaData(locacao.getDataLocacao(), DataUtils.obterData(15, 11, 2021)), is(true));
		error.checkThat(DataUtils.isMesmaData(locacao.getDataRetorno(), DataUtils.obterData(16, 11, 2021)), is(true));

	}

	@Test(expected = FilmeSemEstoqueException.class)
	public void naoDeveAlugarFilmeSemEstoque() throws Exception {

		// cen�rio
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilmeSemEstoque().agora());

		// a��o
		service.alugarFilme(usuario, filmes);
	}

	@Test
	public void naoDeveAlugarFilmeSemUsuario() throws FilmeSemEstoqueException {
		// cenario
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		// acao
		try {
			service.alugarFilme(null, filmes);
			Assert.fail();
		} catch (LocadoraException e) {
			Assert.assertThat(e.getMessage(), is("Usu�rio vazio"));
		}
	}

	@Test
	public void naoDeveAlugarFilmeSemFilme() throws FilmeSemEstoqueException, LocadoraException {

		Usuario usuario = umUsuario().agora();

		exception.expect(LocadoraException.class);
		exception.expectMessage("Filme vazio");

		// a��o
		service.alugarFilme(usuario, null);
	}

	@Test
	public void deveDevolverNaSegundaAoAlugarNoSabado() throws Exception {

		// cen�rio
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		//PowerMockito.whenNew(Date.class).withNoArguments().thenReturn(DataUtils.obterData(20, 11, 2021));
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, 20);
		calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
		calendar.set(Calendar.YEAR, 2021);
		PowerMockito.mockStatic(Calendar.class);
		PowerMockito.when(Calendar.getInstance()).thenReturn(calendar);

		// a��o
		Locacao retorno = service.alugarFilme(usuario, filmes);

		// verifica��o
		// assertThat(retorno.getDataRetorno(), new DiaSemanaMatcher(Calendar.MONDAY));
		assertThat(retorno.getDataRetorno(), caiEm(Calendar.MONDAY));
		assertThat(retorno.getDataRetorno(), caiNumaSegunda());

		//PowerMockito.verifyNew(Date.class, Mockito.times(2)).withNoArguments();
		
		PowerMockito.verifyStatic(Calendar.class, Mockito.times(2));
		Calendar.getInstance();
	}

	/*
	 * public static void main(String[] args) { new
	 * BuilderMaster().gerarCodigoClasse(Locacao.class); }
	 */

	@Test
	public void naoDeveAlugarFilmeParaNegativadoSPC() throws Exception {
		// cen�rio
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		Mockito.when(spc.possuiNegativacao(Mockito.any(Usuario.class))).thenReturn(true);

		// a��o
		try {
			service.alugarFilme(usuario, filmes);

			// verifica��o
			Assert.fail();
		} catch (LocadoraException e) {
			Assert.assertThat(e.getMessage(), is("Usu�rio negativado"));
		}

		Mockito.verify(spc).possuiNegativacao(usuario);
	}

	@Test
	public void deveEnviarEmailParaLocacoesAtrasadas() {
		// cenario
		Usuario usuario = umUsuario().agora();
		Usuario usuario2 = umUsuario().comNome("Usu�rio em dia").agora();
		Usuario usuario3 = umUsuario().comNome("Outro atrasado").agora();
		List<Locacao> locacoes = Arrays.asList(LocacaoBuilder.umLocacao().atrasado().comUsuario(usuario).agora(),
				LocacaoBuilder.umLocacao().comUsuario(usuario2).agora(),
				LocacaoBuilder.umLocacao().atrasado().comUsuario(usuario3).agora(),
				LocacaoBuilder.umLocacao().atrasado().comUsuario(usuario3).agora());
		Mockito.when(dao.obterLocacoesPendentes()).thenReturn(locacoes);

		// a��o
		service.notificarAtrasos();

		// verifica��o
		verify(emailService, Mockito.times(3)).notificarAtraso(Mockito.any(Usuario.class));
		verify(emailService).notificarAtraso(usuario);
		// verify(emailService, Mockito.times(2)).notificarAtraso(usuario3);
		verify(emailService, Mockito.atLeast(1)).notificarAtraso(usuario3);
		verify(emailService, Mockito.never()).notificarAtraso(usuario2);
		verifyNoMoreInteractions(emailService);
	}

	@Test
	public void deveTratarErroNoSPC() throws Exception {
		// cen�rio
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());

		Mockito.when(spc.possuiNegativacao(usuario)).thenThrow(new RuntimeException("Falha catastr�fica!"));

		// verifica��o
		exception.expect(LocadoraException.class);
		// exception.expectMessage("Falha catastr�fica!");
		exception.expectMessage("Problemas com SPC, tente novamente");

		// a��o
		service.alugarFilme(usuario, filmes);

	}

	@Test
	public void deveProrrogarUmaLocacao() {
		// cen�rio
		Locacao locacao = LocacaoBuilder.umLocacao().agora();

		// a��o
		service.prorrogarLocacao(locacao, 3);

		// verificacao
		ArgumentCaptor<Locacao> argCapt = ArgumentCaptor.forClass(Locacao.class);
		Mockito.verify(dao).salvar(argCapt.capture());
		Locacao locacaoRetornada = argCapt.getValue();

		error.checkThat(locacaoRetornada.getValor(), is(12.0));
		error.checkThat(locacaoRetornada.getDataLocacao(), is(ehHoje()));
		error.checkThat(locacaoRetornada.getDataRetorno(), is(ehHojeComDiferencaDias(3)));
	}
	
	@Test
	public void deveAlugarFilme_SemCalcularValor() throws Exception {
		//cen�rio
		Usuario usuario = umUsuario().agora();
		List<Filme> filmes = Arrays.asList(umFilme().agora());
		
		PowerMockito.doReturn(1.0).when(service, "calcularValorLocacao", filmes);
		
		//a��o
		Locacao locacao = service.alugarFilme(usuario, filmes);
		
		//verifica��o
		Assert.assertThat(locacao.getValor(), is(1.0));
		PowerMockito.verifyPrivate(service).invoke("calcularValorLocacao", filmes);
		
	}
	

}

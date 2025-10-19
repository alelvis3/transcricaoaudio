package br.com.alelvis3.transcricaoAudio;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;

import net.miginfocom.swing.MigLayout;


public class MainApp extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
	private JTextField campoCaminhoArquivo; // Campo de texto para o caminho do arquivo
	private JTextArea areaTranscricao; // Área de texto para exibir a transcrição
	private JButton botaoEscolherArquivo; // Botão para escolher o arquivo
	private JButton botaoTranscrever; // Botão para transcrever

	public MainApp() {
		super("Transcrição de Áudio");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Define a operação padrão ao fechar a janela
		setSize(800, 600); // Define o tamanho da janela
		setLocationRelativeTo(null); // Centraliza a janela

		// Layout usando MigLayout
		setLayout(new MigLayout("", "[grow,fill]", "[][grow,fill][]"));

		// Componentes
		campoCaminhoArquivo = new JTextField();
		botaoEscolherArquivo = new JButton("Escolher Arquivo");
		areaTranscricao = new JTextArea();
		areaTranscricao.setLineWrap(true); // Quebra as linhas automaticamente
		areaTranscricao.setWrapStyleWord(true); // Quebra as linhas nas palavras
		JScrollPane scrollPane = new JScrollPane(areaTranscricao); // Para rolar a área de texto
		botaoTranscrever = new JButton("Transcrever");

		// Adicionar componentes ao layout
		add(new JLabel("Arquivo de áudio:"), "split 2, gapright 10");
		add(campoCaminhoArquivo, "growx");
		add(botaoEscolherArquivo, "wrap");
		add(scrollPane, "grow, wrap");
		add(botaoTranscrever, "span, align center");

		// Listeners dos botões
		botaoEscolherArquivo.addActionListener(this::chooseFileAction);
		botaoTranscrever.addActionListener(this::transcribeAction);

		setVisible(true); // Torna a janela visível
	}

	private void chooseFileAction(ActionEvent e) {
		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Arquivos de Áudio MP3", "mp3"); // Filtra
																										// os
																										// tipos
																										// de
																										// arquivo
		fileChooser.setFileFilter(filter);
		int returnVal = fileChooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			campoCaminhoArquivo.setText(selectedFile.getAbsolutePath());
		}
	}

	private void transcribeAction(ActionEvent e) {
		String caminhoArquivoAudio = campoCaminhoArquivo.getText();
		if (caminhoArquivoAudio == null || caminhoArquivoAudio.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Por favor, selecione um arquivo de áudio.", "Erro",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			String transcricao = transcribeAudio(caminhoArquivoAudio);
			areaTranscricao.setText(transcricao);
		} catch (IOException ex) { // Captura IOException (erro de credenciais)
			logger.error("Erro de credenciais: {}", ex.getMessage(), ex);
			JOptionPane.showMessageDialog(this,
					"Erro de credenciais. Verifique a configuração da sua conta de serviço Google Cloud: "
							+ ex.getMessage(),
					"Erro de Autenticação", JOptionPane.ERROR_MESSAGE);
		} catch (Exception ex) {
			logger.error("Erro ao transcrever o áudio: {}", ex.getMessage(), ex);
			JOptionPane.showMessageDialog(this, "Erro ao transcrever o áudio: " + ex.getMessage(), "Erro",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(MainApp::new);
	}

	/**
	 * Transcreve o arquivo de áudio especificado pelo caminho usando a API Google
	 * Cloud Speech-to-Text.
	 *
	 * @param caminhoArquivoAudio O caminho para o arquivo de áudio a ser
	 *                            transcrito.
	 * @return O texto transcrito, ou null se ocorrer um erro.
	 * @throws Exception se houver um erro durante o processo de transcrição.
	 */
	public static String transcribeAudio(String caminhoArquivoAudio) throws Exception {
		Path path = Paths.get(caminhoArquivoAudio);
		byte[] data = Files.readAllBytes(path);
		ByteString audioBytes = ByteString.copyFrom(data);

		// Configurações da requisição
		RecognitionConfig config = RecognitionConfig.newBuilder().setEncoding(AudioEncoding.MP3) // Ajuste isso
																									// conforme o
																									// formato do
																									// seu áudio
																									// (LINEAR16 é
																									// comum para
																									// WAV)
				.setSampleRateHertz(16000) // Ajuste isso para a taxa de amostragem do seu áudio
				.setLanguageCode("pt-BR") // Código do idioma
				.build();
		RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

		// Autenticação
		try {
			// Carrega as credenciais do arquivo JSON especificado na variável de ambiente
			String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
			if (credentialsPath == null || credentialsPath.isEmpty()) {
				throw new IOException("A variável de ambiente GOOGLE_APPLICATION_CREDENTIALS não está configurada.");
			}

			GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
			SpeechSettings speechSettings = SpeechSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();

			// Transcrição
			try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
				RecognizeResponse response = speechClient.recognize(config, audio);
				List<SpeechRecognitionResult> results = response.getResultsList();

				StringBuilder transcricao = new StringBuilder();
				for (SpeechRecognitionResult result : results) {
					SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
					transcricao.append(alternative.getTranscript());
				}
				return transcricao.toString();
			}
		} catch (IOException e) {
			// Propaga a IOException para ser tratada no método transcribeAction
			throw e;
		}
	}
}
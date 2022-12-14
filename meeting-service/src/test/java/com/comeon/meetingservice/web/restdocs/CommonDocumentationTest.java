package com.comeon.meetingservice.web.restdocs;

import com.comeon.meetingservice.domain.meetingplace.entity.PlaceCategory;
import com.comeon.meetingservice.web.ControllerTestBase;
import com.comeon.meetingservice.web.common.response.ApiResponseCode;
import com.comeon.meetingservice.web.common.response.EnumType;
import com.comeon.meetingservice.common.exception.ErrorCode;
import com.comeon.meetingservice.web.restdocs.util.CommonResponseFieldsSnippet;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.PayloadSubsectionExtractor;
import org.springframework.restdocs.snippet.Attributes;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.attributes;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CommonDocumentationTest extends ControllerTestBase {

    @Test
    public void commons() throws Exception {
        Map<Integer, String> errorCodes = Arrays.stream(ErrorCode.values())
                .collect(Collectors.toMap(ErrorCode::getCode, ErrorCode::getMessage));
        FieldDescriptor[] errorCodeDescriptors = errorCodes.entrySet().stream()
                .map(x -> fieldWithPath(String.valueOf(x.getKey())).description(x.getValue()))
                .toArray(FieldDescriptor[]::new);

        Map<String, String> placeCategories = Arrays.stream(PlaceCategory.values())
                .collect(Collectors.toMap(PlaceCategory::name, PlaceCategory::getKorName));
        FieldDescriptor[] placeCategoryDescriptors = placeCategories.entrySet().stream()
                .map(x -> fieldWithPath(String.valueOf(x.getKey())).description(x.getValue()))
                .toArray(FieldDescriptor[]::new);

        mockMvc.perform(
                RestDocumentationRequestBuilders.get("/docs")

        )
                .andExpect(status().isOk())
                .andDo(document("common",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        commonResponseFields("common-response", null,
                                attributes(new Attributes.Attribute("title", "?????? ?????? ??????")),
                                fieldWithPath("responseTime").type(JsonFieldType.STRING).description("?????? ????????? ???????????????."),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("?????? ????????? ???????????????."),
                                subsectionWithPath("data").description("?????? ???????????? ???????????????.")
                        ),
                        commonResponseFields("common-response", beneathPath("data.apiResponseCodes").withSubsectionId("apiResponseCodes"),
                                attributes(new Attributes.Attribute("title", "?????? ??????")),
                                enumConvertFieldDescriptor(getDocs(ApiResponseCode.values()))
                        ),
                        commonResponseFields("common-response", beneathPath("data.errorCodes").withSubsectionId("errorCodes"),
                                attributes(new Attributes.Attribute("title", "?????? ??????")),
                                errorCodeDescriptors
                        ),
                        commonResponseFields("common-response", beneathPath("data.placeCategories").withSubsectionId("placeCategories"),
                                attributes(new Attributes.Attribute("title", "?????? ????????????")),
                                placeCategoryDescriptors
                        ))

                );
    }

    @Test
    public void error() throws Exception {

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/docs/error")
        )
                .andExpect(status().isBadRequest())
                .andDo(document("common-error",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        commonResponseFields("common-response", beneathPath("data").withSubsectionId("data"),
                                attributes(new Attributes.Attribute("title", "?????? ?????? ??????")),
                                fieldWithPath("errorCode").type(JsonFieldType.NUMBER).description("API ???????????? ????????? ?????? ????????? ???????????????."),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("?????? ???????????? ???????????????.")
                        )
                ));
    }

    @Test
    public void list() throws Exception {

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/docs/list")
                )
                .andExpect(status().isOk())
                .andDo(document("common-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        commonResponseFields("common-response", beneathPath("data").withSubsectionId("data"),
                                attributes(new Attributes.Attribute("title", "????????? ?????? ??????")),
                                fieldWithPath("currentSlice").type(JsonFieldType.NUMBER).description("?????? ???????????? ????????? ???????????????. 0?????? ???????????????."),
                                fieldWithPath("sizePerSlice").type(JsonFieldType.NUMBER).description("??? ??????????????? ??? ?????? ????????? ??????????????? ???????????????."),
                                fieldWithPath("numberOfElements").type(JsonFieldType.NUMBER).description("?????? ??????????????? ??? ?????? ????????? ??????????????? ???????????????."),
                                fieldWithPath("hasPrevious").type(JsonFieldType.BOOLEAN).description("?????? ??????????????? ????????? ???????????????."),
                                fieldWithPath("hasNext").type(JsonFieldType.BOOLEAN).description("?????? ??????????????? ????????? ???????????????."),
                                fieldWithPath("contents").type(JsonFieldType.ARRAY).description("??????????????? ????????? ?????? ???????????? ???????????? ???????????????."),
                                fieldWithPath("first").type(JsonFieldType.BOOLEAN).description("?????? ?????????????????? ???????????????."),
                                fieldWithPath("last").type(JsonFieldType.BOOLEAN).description("????????? ?????????????????? ???????????????.")
                        )
                ));
    }


    private static FieldDescriptor[] enumConvertFieldDescriptor(Map<String, String> enumValues) {

        return enumValues.entrySet().stream()
                .map(x -> fieldWithPath(x.getKey()).description(x.getValue()))
                .toArray(FieldDescriptor[]::new);
    }

    private Map<String, String> getDocs(EnumType[] enumTypes) {
        return Arrays.stream(enumTypes)
                .collect(Collectors.toMap(EnumType::getId, EnumType::getText));
    }

    public static CommonResponseFieldsSnippet commonResponseFields(String type,
                                                                   PayloadSubsectionExtractor<?> subsectionExtractor,
                                                                   Map<String, Object> attributes, FieldDescriptor... descriptors) {
        return new CommonResponseFieldsSnippet(type, subsectionExtractor, Arrays.asList(descriptors), attributes
                , true);
    }
}